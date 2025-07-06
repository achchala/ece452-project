from typing import Optional, Dict, Any, List
from ..base_client import BaseSupabaseClient


class ExpenseOperations:
    """Handles all expense-related database operations using the Supabase client."""
    
    def __init__(self, base_client: BaseSupabaseClient):
        self.client = base_client
        self.expenses_table = self.client.get_table_name("expenses")
        self.splits_table = self.client.get_table_name("splits")
    
    def get_user_lent_expenses(self, user_id: str) -> Optional[List[Dict]]:
        """Get all expenses where the user is the creator."""
        expenses = self.client._execute_query(
            table_name=self.expenses_table,
            operation='select',
            filters={'created_by': user_id}
        )
        
        if not expenses:
            return []
        
        # Get splits for each expense with user information
        for expense in expenses:
            expense_id = expense.get('id')
            if expense_id:
                splits = self.client._execute_query(
                    table_name=self.splits_table,
                    operation='select',
                    filters={'expenseid': expense_id}
                ) or []
                
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
                            enriched_split = {
                                'amount_owed': split.get('amount_owed'),
                                'debtor': {
                                    'name': user[0].get('name')
                                }
                            }
                            enriched_splits.append(enriched_split)
                
                expense['splits'] = enriched_splits
        
        return expenses
    
    def get_user_owed_splits(self, user_id: str) -> Optional[List[Dict]]:
        """Get all splits where the user owes money."""
        splits = self.client._execute_query(
            table_name=self.splits_table,
            operation='select',
            filters={'userid': user_id}
        ) or []
        
        # Enrich splits with expense and lender information
        enriched_splits = []
        for split in splits:
            expense_id = split.get('expenseid')
            if expense_id:
                expense = self.client._execute_query(
                    table_name=self.expenses_table,
                    operation='select',
                    filters={'id': expense_id}
                )
                
                if expense:
                    expense_data = expense[0]
                    lender_id = expense_data.get('created_by')
                    
                    # Get lender information
                    lender = None
                    if lender_id:
                        lender_user = self.client._execute_query(
                            table_name=self.client.get_table_name("users"),
                            operation='select',
                            filters={'id': lender_id}
                        )
                        if lender_user:
                            lender = {
                                'name': lender_user[0].get('name')
                            }
                    
                    enriched_split = {
                        'id': split.get('id'),
                        'expenseid': split.get('expenseid'),
                        'userid': split.get('userid'),
                        'amount_owed': split.get('amount_owed'),
                        'expense': {
                            'title': expense_data.get('title'),
                            'lender': lender
                        }
                    }
                    enriched_splits.append(enriched_split)
        
        return enriched_splits
    
    def get_expense_with_splits(self, expense_id: int) -> Optional[Dict]:
        """Get an expense with all its splits."""
        expense = self.client._execute_query(
            table_name=self.expenses_table,
            operation='select',
            filters={'id': expense_id}
        )
        
        if not expense:
            return None
            
        expense = expense[0]
        
        # Get splits for this expense
        splits = self.client._execute_query(
            table_name=self.splits_table,
            operation='select',
            filters={'expenseid': expense_id}
        )
        
        expense['splits'] = splits if splits else []
        return expense
    
    def create_expense(self, title: str, total_amount: int, created_by: str, group_id: str = None) -> Optional[Dict]:
        """Create a new expense."""
        data = {
            "title": title,
            "total_amount": total_amount,
            "created_by": created_by
        }
        if group_id:
            data["group_id"] = group_id
        return self.client._execute_query(
            table_name=self.expenses_table,
            operation='insert',
            data=data
        )
    
    def create_split(self, expense_id: int, user_id: str, amount_owed: int) -> Optional[Dict]:
        """Create a new split for an expense."""
        data = {
            "expenseid": expense_id,
            "userid": user_id,
            "amount_owed": amount_owed
        }
        return self.client._execute_query(
            table_name=self.splits_table,
            operation='insert',
            data=data
        )
    
    def get_user_dashboard_data(self, user_id: str) -> Dict[str, Any]:
        """Get dashboard data for a user including lent and owed amounts."""
        # Get expenses where user lent money
        lent_expenses = self.get_user_lent_expenses(user_id) or []
        
        # Get splits where user owes money
        owed_splits = self.get_user_owed_splits(user_id) or []
        
        # Calculate total amounts
        total_lent = sum(expense.get('total_amount', 0) for expense in lent_expenses)
        total_owed = sum(split.get('amount_owed', 0) for split in owed_splits)
        
        return {
            "lent": {
                "total_amount": total_lent,
                "expenses": lent_expenses
            },
            "owed": {
                "total_amount": total_owed,
                "splits": owed_splits
            }
        }
    
    def update_expense(self, expense_id: int, data: Dict[str, Any]) -> Optional[Dict]:
        """Update an expense."""
        return self.client._execute_query(
            table_name=self.expenses_table,
            operation='update',
            data=data,
            filters={'id': expense_id}
        )
    
    def delete_expense(self, expense_id: int) -> bool:
        """Delete an expense and all its splits."""
        # First delete all splits for this expense
        self.client._execute_query(
            table_name=self.splits_table,
            operation='delete',
            filters={'expenseid': expense_id}
        )
        
        # Then delete the expense
        return self.client._execute_query(
            table_name=self.expenses_table,
            operation='delete',
            filters={'id': expense_id}
        )
    
    def get_group_expenses(self, group_id: str) -> Optional[List[Dict]]:
        """Get all expenses for a specific group."""
        expenses = self.client._execute_query(
            table_name=self.expenses_table,
            operation='select',
            filters={'group_id': group_id}
        )
        
        if not expenses:
            return []
        
        # Get splits for each expense
        for expense in expenses:
            expense_id = expense.get('id')
            if expense_id:
                splits = self.client._execute_query(
                    table_name=self.splits_table,
                    operation='select',
                    filters={'expenseid': expense_id}
                )
                expense['splits'] = splits if splits else []
        
        return expenses
    
    def get_user_group_expenses(self, user_id: str, group_id: str) -> Optional[List[Dict]]:
        """Get all expenses for a user in a specific group."""
        # Get expenses created by the user in this group
        created_expenses = self.client._execute_query(
            table_name=self.expenses_table,
            operation='select',
            filters={'created_by': user_id, 'group_id': group_id}
        ) or []
        
        # Get expenses where the user owes money in this group
        owed_splits = self.client._execute_query(
            table_name=self.splits_table,
            operation='select',
            filters={'userid': user_id}
        ) or []
        
        # Get the actual expenses for the owed splits in this group
        owed_expenses = []
        for split in owed_splits:
            expense_id = split.get('expenseid')
            if expense_id:
                expense = self.client._execute_query(
                    table_name=self.expenses_table,
                    operation='select',
                    filters={'id': expense_id, 'group_id': group_id}
                )
                if expense:
                    expense[0]['splits'] = [split]
                    owed_expenses.append(expense[0])
        
        return {
            'created': created_expenses,
            'owed': owed_expenses
        }