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
        return self.client._execute_query(
            table_name=self.expenses_table,
            operation='select',
            filters={'created_by': user_id}
        )
    
    def get_user_owed_splits(self, user_id: str) -> Optional[List[Dict]]:
        """Get all splits where the user owes money."""
        return self.client._execute_query(
            table_name=self.splits_table,
            operation='select',
            filters={'userId': user_id}
        )
    
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
            filters={'expenseId': expense_id}
        )
        
        expense['splits'] = splits if splits else []
        return expense
    
    def create_expense(self, title: str, total_amount: int, created_by: str) -> Optional[Dict]:
        """Create a new expense."""
        data = {
            "title": title,
            "total_amount": total_amount,
            "created_by": created_by
        }
        return self.client._execute_query(
            table_name=self.expenses_table,
            operation='insert',
            data=data
        )
    
    def create_split(self, expense_id: int, user_id: str, amount_owed: int) -> Optional[Dict]:
        """Create a new split for an expense."""
        data = {
            "expenseId": expense_id,
            "userId": user_id,
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
            filters={'expenseId': expense_id}
        )
        
        # Then delete the expense
        return self.client._execute_query(
            table_name=self.expenses_table,
            operation='delete',
            filters={'id': expense_id}
        )