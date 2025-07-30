from typing import Optional, Dict, Any, List
from ..base_client import BaseSupabaseClient


class ExpenseOperations:
    """Handles all expense-related database operations using the Supabase client."""

    def __init__(self, base_client: BaseSupabaseClient):
        self.client = base_client
        self.expenses_table = self.client.get_table_name("expenses")
        self.splits_table = self.client.get_table_name("splits")

    def get_user_lent_expenses(self, user_id: str) -> Optional[List[Dict]]:
        """Get all expenses where the user is the creator and at least one split is not fully paid."""
        expenses = self.client._execute_query(
            table_name=self.expenses_table,
            operation="select",
            filters={"created_by": user_id},
        )

        if not expenses:
            return []
        
        # Filter expenses to only include those with at least one unpaid split
        filtered_expenses = []
        
        for expense in expenses:
            expense_id = expense.get("id")
            if expense_id:
                splits = self.client._execute_query(
                    table_name=self.splits_table,
                    operation='select',
                    filters={'expenseid': expense_id}
                ) or []
                
                # Check if all splits are confirmed as paid
                all_paid = True
                if splits:
                    for split in splits:
                        if split.get('paid_confirmed') is None:
                            all_paid = False
                            break
                else:
                    # If no splits exist, consider it as not fully paid
                    all_paid = False
                
                # Only include expenses that are not fully paid
                if not all_paid:
                    # Enrich splits with user information
                    enriched_splits = []
                    for split in splits:
                        split_user_id = split.get('userid')
                        if split_user_id:
                            user = self.client._execute_query(
                                table_name=self.client.get_table_name("users"),
                                operation='select',
                                filters={'id': split_user_id}
                            )
                            if user:
                                # Determine payment status
                                payment_status = None
                                if split.get('paid_confirmed') is not None:
                                    payment_status = 'paid'  # Green check
                                elif split.get('paid_request') is not None:
                                    payment_status = 'pending'  # Hourglass
                                # If neither is set, payment_status remains None (no icon)
                                
                                enriched_split = {
                                    'id': split.get('id'),
                                    'amount_owed': split.get('amount_owed'),
                                    'paid_request': split.get('paid_request'),
                                    'paid_confirmed': split.get('paid_confirmed'),
                                    'debtor': {
                                        'name': user[0].get('name'),
                                        'payment_status': payment_status
                                    }
                                }
                                enriched_splits.append(enriched_split)
                    
                    expense['splits'] = enriched_splits
                    filtered_expenses.append(expense)
        
        return filtered_expenses
    
    def get_user_owed_splits(self, user_id: str) -> Optional[List[Dict]]:
        """Get all splits where the user owes money and payment has not been confirmed."""
        splits = self.client._execute_query(
            table_name=self.splits_table,
            operation='select',
            filters={'userid': user_id}
        ) or []
        
        # Filter out splits that have been confirmed as paid
        splits = [split for split in splits if split.get('paid_confirmed') is None]
        
        # Enrich splits with expense and lender information
        enriched_splits = []
        for split in splits:
            expense_id = split.get("expenseid")
            if expense_id:
                expense = self.client._execute_query(
                    table_name=self.expenses_table,
                    operation="select",
                    filters={"id": expense_id},
                )

                if expense:
                    expense_data = expense[0]
                    lender_id = expense_data.get("created_by")

                    # Get lender information
                    lender = None
                    if lender_id:
                        lender_user = self.client._execute_query(
                            table_name=self.client.get_table_name("users"),
                            operation="select",
                            filters={"id": lender_id},
                        )
                        if lender_user:
                            lender = {"name": lender_user[0].get("name")}

                    enriched_split = {
                        'id': split.get('id'),
                        'expenseid': split.get('expenseid'),
                        'userid': split.get('userid'),
                        'amount_owed': split.get('amount_owed'),
                        'paid_request': split.get('paid_request'),
                        'paid_confirmed': split.get('paid_confirmed'),
                        'expense': {
                            'title': expense_data.get('title'),
                            'due_date': expense_data.get('due_date'),
                            'lender': lender
                        }
                    }
                    enriched_splits.append(enriched_split)

        return enriched_splits

    def get_expense_with_splits(self, expense_id: str) -> Optional[Dict]:
        """Get an expense with all its splits."""
        expense = self.client._execute_query(
            table_name=self.expenses_table,
            operation="select",
            filters={"id": expense_id},
        )

        if not expense:
            return None

        expense = expense[0]

        # Get splits for this expense
        splits = self.client._execute_query(
            table_name=self.splits_table,
            operation="select",
            filters={"expenseid": expense_id},
        )
        
        # Enrich splits with user information and payment status
        enriched_splits = []
        if splits:
            for split in splits:
                split_user_id = split.get('userid')
                if split_user_id:
                    user = self.client._execute_query(
                        table_name=self.client.get_table_name("users"),
                        operation='select',
                        filters={'id': split_user_id}
                    )
                    if user:
                        # Determine payment status
                        payment_status = None
                        if split.get('paid_confirmed') is not None:
                            payment_status = 'paid'  # Green check
                        elif split.get('paid_request') is not None:
                            payment_status = 'pending'  # Hourglass
                        # If neither is set, payment_status remains None (no icon)
                        
                        enriched_split = {
                            'id': split.get('id'),
                            'amount_owed': split.get('amount_owed'),
                            'paid_request': split.get('paid_request'),
                            'paid_confirmed': split.get('paid_confirmed'),
                            'debtor': {
                                'name': user[0].get('name'),
                                'payment_status': payment_status
                            }
                        }
                        enriched_splits.append(enriched_split)
                    else:
                        # If user not found, add split without debtor info
                        enriched_splits.append(split)
                else:
                    # If no user_id, add split as-is
                    enriched_splits.append(split)
        
        expense['splits'] = enriched_splits
        return expense

    def create_expense(
        self,
        title: str,
        total_amount: int,
        created_by: str,
        group_id: str = None,
        due_date: str = None,
        category: str = None,
    ) -> Optional[Dict]:
        """Create a new expense."""
        data = {"title": title, "total_amount": total_amount, "created_by": created_by}
        if group_id:
            data["group_id"] = group_id
        if due_date:
            data["due_date"] = due_date
        if category:
            data["category"] = category
        return self.client._execute_query(
            table_name=self.expenses_table, operation="insert", data=data
        )

    def create_split(
        self, expense_id: str, user_id: str, amount_owed: int
    ) -> Optional[Dict]:
        """Create a new split for an expense."""
        data = {"expenseid": expense_id, "userid": user_id, "amount_owed": amount_owed}
        return self.client._execute_query(
            table_name=self.splits_table, operation="insert", data=data
        )

    def get_user_dashboard_data(self, user_id: str) -> Dict[str, Any]:
        """Get dashboard data for a user including lent and owed amounts."""
        # Get expenses where user lent money
        lent_expenses = self.get_user_lent_expenses(user_id) or []

        # Get splits where user owes money
        owed_splits = self.get_user_owed_splits(user_id) or []

        # Calculate total amounts
        total_lent = sum(expense.get("total_amount", 0) for expense in lent_expenses)
        total_owed = sum(split.get("amount_owed", 0) for split in owed_splits)

        # Calculate net amount (lent - owed)
        net_amount = total_lent - total_owed

        return {
            "lent": {"total_amount": total_lent, "expenses": lent_expenses},
            "owed": {"total_amount": total_owed, "splits": owed_splits},
            "net": {"total_amount": net_amount},
        }

    def update_expense(self, expense_id: str, data: Dict[str, Any]) -> Optional[Dict]:
        """Update an expense."""
        return self.client._execute_query(
            table_name=self.expenses_table,
            operation="update",
            data=data,
            filters={"id": expense_id},
        )

    def delete_expense(self, expense_id: str) -> bool:
        """Delete an expense and all its splits."""
        # First delete all splits for this expense
        self.client._execute_query(
            table_name=self.splits_table,
            operation="delete",
            filters={"expenseid": expense_id},
        )

        # Then delete the expense
        return self.client._execute_query(
            table_name=self.expenses_table,
            operation="delete",
            filters={"id": expense_id},
        )

    def update_group_budget_after_expense(
        self, group_id: str, expense_amount: int
    ) -> bool:
        """Update group budget by subtracting the expense amount."""
        # Get current group budget
        group = self.client._execute_query(
            table_name=self.client.get_table_name("groups"),
            operation="select",
            filters={"id": group_id},
        )

        if not group:
            return False

        current_budget = group[0].get("total_budget")
        if current_budget is None:
            return True  # No budget set, nothing to update

        # Calculate new budget
        new_budget = current_budget - (
            expense_amount / 100.0
        )  # Convert cents to dollars

        # Update group budget
        return self.client._execute_query(
            table_name=self.client.get_table_name("groups"),
            operation="update",
            data={"total_budget": new_budget},
            filters={"id": group_id},
        )

    def get_group_expenses(self, group_id: str) -> Optional[List[Dict]]:
        """Get all expenses for a specific group."""
        expenses = self.client._execute_query(
            table_name=self.expenses_table,
            operation="select",
            filters={"group_id": group_id},
        )

        if not expenses:
            return []

        # Get splits for each expense
        for expense in expenses:
            expense_id = expense.get("id")
            if expense_id:
                splits = self.client._execute_query(
                    table_name=self.splits_table,
                    operation="select",
                    filters={"expenseid": expense_id},
                )
                
                # Enrich splits with user information and payment status
                enriched_splits = []
                if splits:
                    for split in splits:
                        split_user_id = split.get('userid')
                        if split_user_id:
                            user = self.client._execute_query(
                                table_name=self.client.get_table_name("users"),
                                operation='select',
                                filters={'id': split_user_id}
                            )
                            if user:
                                # Determine payment status
                                payment_status = None
                                if split.get('paid_confirmed') is not None:
                                    payment_status = 'paid'  # Green check
                                elif split.get('paid_request') is not None:
                                    payment_status = 'pending'  # Hourglass
                                # If neither is set, payment_status remains None (no icon)
                                
                                enriched_split = {
                                    'id': split.get('id'),
                                    'amount_owed': split.get('amount_owed'),
                                    'paid_request': split.get('paid_request'),
                                    'paid_confirmed': split.get('paid_confirmed'),
                                    'debtor': {
                                        'name': user[0].get('name'),
                                        'payment_status': payment_status
                                    }
                                }
                                enriched_splits.append(enriched_split)
                            else:
                                # If user not found, add split without debtor info
                                enriched_splits.append(split)
                        else:
                            # If no user_id, add split as-is
                            enriched_splits.append(split)
                
                expense['splits'] = enriched_splits
        
        return expenses

    def get_user_group_expenses(
        self, user_id: str, group_id: str
    ) -> Optional[List[Dict]]:
        """Get all expenses for a user in a specific group."""
        # Get expenses created by the user in this group
        created_expenses = self.client._execute_query(
            table_name=self.expenses_table,
            operation='select',
            filters={'created_by': user_id, 'group_id': group_id}
        ) or []
        
        # Filter out fully paid expenses from created expenses
        filtered_created_expenses = []
        for expense in created_expenses:
            expense_id = expense.get('id')
            if expense_id:
                splits = self.client._execute_query(
                    table_name=self.splits_table,
                    operation='select',
                    filters={'expenseid': expense_id}
                ) or []
                
                # Check if all splits are confirmed as paid
                all_paid = True
                if splits:
                    for split in splits:
                        if split.get('paid_confirmed') is None:
                            all_paid = False
                            break
                else:
                    # If no splits exist, consider it as not fully paid
                    all_paid = False
                
                # Only include expenses that are not fully paid
                if not all_paid:
                    # Enrich splits with user information and payment status
                    enriched_splits = []
                    for split in splits:
                        split_user_id = split.get('userid')
                        if split_user_id:
                            user = self.client._execute_query(
                                table_name=self.client.get_table_name("users"),
                                operation='select',
                                filters={'id': split_user_id}
                            )
                            if user:
                                # Determine payment status
                                payment_status = None
                                if split.get('paid_confirmed') is not None:
                                    payment_status = 'paid'  # Green check
                                elif split.get('paid_request') is not None:
                                    payment_status = 'pending'  # Hourglass
                                # If neither is set, payment_status remains None (no icon)
                                
                                enriched_split = {
                                    'id': split.get('id'),
                                    'amount_owed': split.get('amount_owed'),
                                    'paid_request': split.get('paid_request'),
                                    'paid_confirmed': split.get('paid_confirmed'),
                                    'debtor': {
                                        'name': user[0].get('name'),
                                        'payment_status': payment_status
                                    }
                                }
                                enriched_splits.append(enriched_split)
                    
                    expense['splits'] = enriched_splits
                    filtered_created_expenses.append(expense)
        
        # Get expenses where the user owes money in this group
        owed_splits = self.client._execute_query(
            table_name=self.splits_table,
            operation='select',
            filters={'userid': user_id}
        ) or []
        
        # Filter out splits that have been confirmed as paid
        owed_splits = [split for split in owed_splits if split.get('paid_confirmed') is None]
        
        # Get the actual expenses for the owed splits in this group
        owed_expenses = []
        for split in owed_splits:
            expense_id = split.get("expenseid")
            if expense_id:
                expense = self.client._execute_query(
                    table_name=self.expenses_table,
                    operation="select",
                    filters={"id": expense_id, "group_id": group_id},
                )
                if expense:
                    expense[0]["splits"] = [split]
                    owed_expenses.append(expense[0])
        
        return {
            'created': filtered_created_expenses,
            'owed': owed_expenses
        }

    def request_payment_confirmation(self, split_id: str, user_id: str) -> Optional[Dict]:
        """Request payment confirmation for a split. Sets paid_request timestamp."""
        from datetime import datetime
        
        # Verify the user owns this split (they owe money)
        split = self.client._execute_query(
            table_name=self.splits_table,
            operation='select',
            filters={'id': split_id, 'userid': user_id}
        )
        
        if not split:
            return None
        
        # Update the split with paid_request timestamp
        update_data = {
            'paid_request': datetime.utcnow().isoformat()
        }
        
        result = self.client._execute_query(
            table_name=self.splits_table,
            operation='update',
            filters={'id': split_id},
            data=update_data
        )
        
        # Handle both list and dict return types
        if isinstance(result, list) and len(result) > 0:
            return result[0]
        elif isinstance(result, dict):
            return result
        else:
            return None

    def confirm_payment(self, split_id: str, lender_id: str) -> Optional[Dict]:
        """Confirm payment for a split. Sets paid_confirmed timestamp."""
        from datetime import datetime
        
        # Get the split to verify the lender owns the expense
        split = self.client._execute_query(
            table_name=self.splits_table,
            operation='select',
            filters={'id': split_id}
        )
        
        if not split:
            return None
        
        split_data = split[0]
        expense_id = split_data.get('expenseid')
        
        # Verify the lender owns this expense
        expense = self.client._execute_query(
            table_name=self.expenses_table,
            operation='select',
            filters={'id': expense_id, 'created_by': lender_id}
        )
        
        if not expense:
            return None
        
        # Update the split with paid_confirmed timestamp
        update_data = {
            'paid_confirmed': datetime.utcnow().isoformat()
        }
        
        result = self.client._execute_query(
            table_name=self.splits_table,
            operation='update',
            filters={'id': split_id},
            data=update_data
        )
        
        # Handle both list and dict return types
        if isinstance(result, list) and len(result) > 0:
            return result[0]
        elif isinstance(result, dict):
            return result
        else:
            return None

    def reject_payment(self, split_id: str, lender_id: str) -> Optional[Dict]:
        """Reject payment for a split. Removes paid_request timestamp."""
        # Get the split to verify the lender owns the expense
        split = self.client._execute_query(
            table_name=self.splits_table,
            operation='select',
            filters={'id': split_id}
        )
        
        if not split:
            return None
        
        split_data = split[0]
        expense_id = split_data.get('expenseid')
        
        # Verify the lender owns this expense
        expense = self.client._execute_query(
            table_name=self.expenses_table,
            operation='select',
            filters={'id': expense_id, 'created_by': lender_id}
        )
        
        if not expense:
            return None
        
        # Update the split to remove paid_request timestamp
        update_data = {
            'paid_request': None
        }
        
        result = self.client._execute_query(
            table_name=self.splits_table,
            operation='update',
            filters={'id': split_id},
            data=update_data
        )
        
        # Handle both list and dict return types
        if isinstance(result, list) and len(result) > 0:
            return result[0]
        elif isinstance(result, dict):
            return result
        else:
            return None

    def get_pending_payment_requests(self, lender_id: str) -> Optional[List[Dict]]:
        """Get all splits with pending payment requests for a lender."""
        # Get all expenses created by the lender
        expenses = self.client._execute_query(
            table_name=self.expenses_table,
            operation='select',
            filters={'created_by': lender_id}
        ) or []
        
        expense_ids = [expense.get('id') for expense in expenses]
        
        if not expense_ids:
            return []
        
        # Get splits with paid_request but no paid_confirmed
        pending_splits = []
        for expense_id in expense_ids:
            splits = self.client._execute_query(
                table_name=self.splits_table,
                operation='select',
                filters={
                    'expenseid': expense_id,
                    'paid_request__not': None,
                    'paid_confirmed__is': None
                }
            ) or []
            
            for split in splits:
                # Get debtor information
                debtor_id = split.get('userid')
                debtor = self.client._execute_query(
                    table_name=self.client.get_table_name("users"),
                    operation='select',
                    filters={'id': debtor_id}
                )
                
                # Get expense information
                expense = self.client._execute_query(
                    table_name=self.expenses_table,
                    operation='select',
                    filters={'id': expense_id}
                )
                
                if debtor and expense:
                    enriched_split = {
                        'id': split.get('id'),
                        'amount_owed': split.get('amount_owed'),
                        'paid_request': split.get('paid_request'),
                        'debtor': {
                            'name': debtor[0].get('name'),
                            'id': debtor_id
                        },
                        'expense': {
                            'title': expense[0].get('title'),
                            'id': expense_id
                        }
                    }
                    pending_splits.append(enriched_split)
        
        return pending_splits

    def get_user_expenses(self, user_id: str) -> Optional[List[Dict]]:
        """Get all expenses for a user (both created and owed)."""
        # Get expenses created by the user
        created_expenses = self.client._execute_query(
            table_name=self.expenses_table,
            operation='select',
            filters={'created_by': user_id}
        ) or []
        
        # Get expenses where the user owes money
        owed_splits = self.client._execute_query(
            table_name=self.splits_table,
            operation='select',
            filters={'userid': user_id}
        ) or []
        
        owed_expenses = []
        for split in owed_splits:
            expense_id = split.get("expenseid")
            if expense_id:
                expense = self.client._execute_query(
                    table_name=self.expenses_table,
                    operation="select",
                    filters={"id": expense_id},
                )
                if expense:
                    owed_expenses.append(expense[0])
        
        # Combine and remove duplicates
        all_expenses = created_expenses + owed_expenses
        unique_expenses = {}
        for expense in all_expenses:
            expense_id = expense.get('id')
            if expense_id not in unique_expenses:
                unique_expenses[expense_id] = expense
        
        return list(unique_expenses.values())
