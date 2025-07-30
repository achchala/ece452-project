from typing import Optional, Dict, Any, List
from datetime import datetime, timedelta
import math
from ..base_client import BaseSupabaseClient


class CreditScoreOperations:
    """Handles credit score calculations and updates."""

    def __init__(self, base_client: BaseSupabaseClient):
        self.client = base_client
        self.expenses_table = self.client.get_table_name("expenses")
        self.splits_table = self.client.get_table_name("splits")
        self.users_table = self.client.get_table_name("users")

    def calculate_user_credit_score(self, user_id: str) -> Optional[int]:
        """
        Calculate credit score for a user based on their payment history.
        
        Algorithm:
        - Payment History (40%): On-time payments, late payments, unpaid debts
        - Payment Behavior (30%): Request frequency, confirmation rate, avg time
        - Debt Utilization (20%): Current outstanding debt
        - Payment Patterns (10%): Consistency and frequency
        
        Returns:
        - Score between 300-850 (FICO-like scale)
        - None if user has no payment history
        """
        
        # Get all splits for the user
        user_splits = self.client._execute_query(
            table_name=self.splits_table,
            operation='select',
            filters={'userid': user_id}
        ) or []
        
        if not user_splits:
            return None  # No payment history
        
        # Get expense details for each split
        enriched_splits = []
        for split in user_splits:
            expense_id = split.get('expenseid')
            if expense_id:
                expense = self.client._execute_query(
                    table_name=self.expenses_table,
                    operation='select',
                    filters={'id': expense_id}
                )
                if expense:
                    split['expense'] = expense[0]
                    enriched_splits.append(split)
        
        if not enriched_splits:
            return None
        
        # Calculate individual factors
        payment_history_score = self._calculate_payment_history_score(enriched_splits)
        payment_behavior_score = self._calculate_payment_behavior_score(enriched_splits)
        debt_utilization_score = self._calculate_debt_utilization_score(enriched_splits)
        payment_patterns_score = self._calculate_payment_patterns_score(enriched_splits)
        
        # Weighted average (0-100 scale)
        weighted_score = (
            payment_history_score * 0.40 +
            payment_behavior_score * 0.30 +
            debt_utilization_score * 0.20 +
            payment_patterns_score * 0.10
        )
        
        # Convert to 300-850 scale (like FICO scores)
        # Base score of 300 + scaled component (0-550 range)
        credit_score = 300 + int(weighted_score * 5.5)
        
        return credit_score
    
    def _calculate_payment_history_score(self, splits: List[Dict]) -> float:
        """Calculate payment history score (0-100)."""
        if not splits:
            return 0.0
        
        total_splits = len(splits)
        on_time_payments = 0
        late_payments = 0
        unpaid_debts = 0
        
        for split in splits:
            expense = split.get('expense', {})
            due_date_str = expense.get('due_date')
            paid_confirmed = split.get('paid_confirmed')
            
            if paid_confirmed:
                # Payment was made
                if due_date_str:
                    try:
                        # Ensure timezone-aware datetime objects
                        due_date_str_clean = due_date_str.replace('Z', '+00:00') if 'Z' in due_date_str else due_date_str
                        paid_confirmed_clean = paid_confirmed.replace('Z', '+00:00') if 'Z' in paid_confirmed else paid_confirmed
                        
                        due_date = datetime.fromisoformat(due_date_str_clean)
                        paid_date = datetime.fromisoformat(paid_confirmed_clean)
                        
                        # Allow 7 days grace period
                        grace_period = due_date + timedelta(days=7)
                        
                        if paid_date <= grace_period:
                            on_time_payments += 1
                        else:
                            late_payments += 1
                    except (ValueError, TypeError):
                        # Skip invalid datetime formats
                        continue
                else:
                    # No due date, consider on-time if paid within 30 days of expense creation
                    expense_created_str = expense.get('created_at', '')
                    if expense_created_str:
                        try:
                            # Ensure timezone-aware datetime objects
                            expense_created_clean = expense_created_str.replace('Z', '+00:00') if 'Z' in expense_created_str else expense_created_str
                            paid_confirmed_clean = paid_confirmed.replace('Z', '+00:00') if 'Z' in paid_confirmed else paid_confirmed
                            
                            expense_created = datetime.fromisoformat(expense_created_clean)
                            paid_date = datetime.fromisoformat(paid_confirmed_clean)
                            
                            if (paid_date - expense_created).days <= 30:
                                on_time_payments += 1
                            else:
                                late_payments += 1
                        except (ValueError, TypeError):
                            # Skip invalid datetime formats
                            continue
                    else:
                        # If no created_at, consider it on-time
                        on_time_payments += 1
            else:
                # Unpaid debt
                unpaid_debts += 1
        
        # Calculate score based on ratios
        on_time_ratio = on_time_payments / total_splits if total_splits > 0 else 0
        late_ratio = late_payments / total_splits if total_splits > 0 else 0
        unpaid_ratio = unpaid_debts / total_splits if total_splits > 0 else 0
        
        # Score calculation: 100 for all on-time, 0 for all unpaid
        score = (on_time_ratio * 100) + (late_ratio * 50) + (unpaid_ratio * 0)
        
        return max(0, min(100, score))
    
    def _calculate_payment_behavior_score(self, splits: List[Dict]) -> float:
        """Calculate payment behavior score (0-100)."""
        if not splits:
            return 0.0
        
        total_splits = len(splits)
        paid_splits = [s for s in splits if s.get('paid_confirmed')]
        
        if not paid_splits:
            return 0.0
        
        # Calculate average time between paid_request and paid_confirmed
        request_to_confirmation_times = []
        for split in paid_splits:
            paid_request = split.get('paid_request')
            paid_confirmed = split.get('paid_confirmed')
            
            if paid_request and paid_confirmed:
                try:
                    # Ensure timezone-aware datetime objects
                    paid_request_clean = paid_request.replace('Z', '+00:00') if 'Z' in paid_request else paid_request
                    paid_confirmed_clean = paid_confirmed.replace('Z', '+00:00') if 'Z' in paid_confirmed else paid_confirmed
                    
                    request_time = datetime.fromisoformat(paid_request_clean)
                    confirmed_time = datetime.fromisoformat(paid_confirmed_clean)
                    time_diff = (confirmed_time - request_time).total_seconds() / 3600  # hours
                    request_to_confirmation_times.append(time_diff)
                except (ValueError, TypeError):
                    # Skip invalid datetime formats
                    continue
        
        # Calculate confirmation rate
        confirmation_rate = len(paid_splits) / total_splits
        
        # Calculate average confirmation time (lower is better)
        avg_confirmation_time = sum(request_to_confirmation_times) / len(request_to_confirmation_times) if request_to_confirmation_times else 0
        
        # Score based on confirmation rate and speed
        # Higher confirmation rate and faster confirmation = higher score
        confirmation_score = confirmation_rate * 60  # 0-60 points
        speed_score = max(0, 40 - (avg_confirmation_time / 24))  # 0-40 points (faster = higher)
        
        return max(0, min(100, confirmation_score + speed_score))
    
    def _calculate_debt_utilization_score(self, splits: List[Dict]) -> float:
        """Calculate debt utilization score (0-100)."""
        if not splits:
            return 100.0  # No debt = perfect score
        
        # Calculate current outstanding debt
        current_debt = sum(
            split.get('amount_owed', 0) 
            for split in splits 
            if not split.get('paid_confirmed')
        )
        
        # Calculate total historical debt
        total_historical_debt = sum(split.get('amount_owed', 0) for split in splits)
        
        if total_historical_debt == 0:
            return 100.0
        
        # Calculate utilization ratio
        utilization_ratio = current_debt / total_historical_debt
        
        # Score: 100 for 0% utilization, 0 for 100% utilization
        # Use a curve that penalizes high utilization more heavily
        if utilization_ratio <= 0.1:
            score = 100
        elif utilization_ratio <= 0.3:
            score = 80
        elif utilization_ratio <= 0.5:
            score = 60
        elif utilization_ratio <= 0.7:
            score = 30
        else:
            score = 0
        
        return score
    
    def _calculate_payment_patterns_score(self, splits: List[Dict]) -> float:
        """Calculate payment patterns score (0-100)."""
        if not splits:
            return 0.0
        
        # Calculate frequency of payment requests
        payment_requests = [s for s in splits if s.get('paid_request')]
        request_frequency = len(payment_requests) / len(splits) if splits else 0
        
        # Calculate consistency (standard deviation of payment times)
        paid_splits = [s for s in splits if s.get('paid_confirmed')]
        if len(paid_splits) < 2:
            return 50.0  # Neutral score for insufficient data
        
        payment_times = []
        for split in paid_splits:
            expense = split.get('expense', {})
            created_at = expense.get('created_at')
            paid_confirmed = split.get('paid_confirmed')
            
            if created_at and paid_confirmed:
                try:
                    # Ensure timezone-aware datetime objects
                    created_at_clean = created_at.replace('Z', '+00:00') if 'Z' in created_at else created_at
                    paid_confirmed_clean = paid_confirmed.replace('Z', '+00:00') if 'Z' in paid_confirmed else paid_confirmed
                    
                    created_time = datetime.fromisoformat(created_at_clean)
                    paid_time = datetime.fromisoformat(paid_confirmed_clean)
                    payment_time = (paid_time - created_time).total_seconds() / 3600  # hours
                    payment_times.append(payment_time)
                except (ValueError, TypeError):
                    # Skip invalid datetime formats
                    continue
        
        if len(payment_times) < 2:
            return 50.0
        
        # Calculate consistency (lower standard deviation = higher score)
        mean_time = sum(payment_times) / len(payment_times)
        variance = sum((t - mean_time) ** 2 for t in payment_times) / len(payment_times)
        std_dev = math.sqrt(variance)
        
        # Score based on consistency and request frequency
        consistency_score = max(0, 60 - (std_dev / 24))  # 0-60 points
        frequency_score = min(40, request_frequency * 100)  # 0-40 points
        
        return max(0, min(100, consistency_score + frequency_score))
    
    def update_user_credit_score(self, user_id: str) -> Optional[Dict]:
        """Calculate and update user's credit score in the database."""
        credit_score = self.calculate_user_credit_score(user_id)
        
        if credit_score is None:
            # User has no payment history, set credit_score to NULL
            result = self.client._execute_query(
                table_name=self.users_table,
                operation='update',
                data={'credit_score': None},
                filters={'id': user_id}
            )
            return {'credit_score': None, 'message': 'No payment history'}
        
        # Update the user's credit score
        result = self.client._execute_query(
            table_name=self.users_table,
            operation='update',
            data={'credit_score': credit_score},
            filters={'id': user_id}
        )
        
        if result:
            return {
                'credit_score': credit_score,
                'message': 'Credit score updated successfully'
            }
        else:
            return None
    
    def get_user_credit_score(self, user_id: str) -> Optional[Dict]:
        """Get user's current credit score."""
        user = self.client._execute_query(
            table_name=self.users_table,
            operation='select',
            filters={'id': user_id}
        )
        
        if user:
            return {
                'credit_score': user[0].get('credit_score'),
                'user_id': user_id
            }
        return None
    
    def update_all_credit_scores(self) -> Dict[str, Any]:
        """Update credit scores for all users."""
        users = self.client._execute_query(
            table_name=self.users_table,
            operation='select'
        ) or []
        
        results = {
            'total_users': len(users),
            'updated_users': 0,
            'failed_users': 0,
            'users_with_scores': 0,
            'users_without_history': 0
        }
        
        for user in users:
            user_id = user.get('id')
            if user_id:
                result = self.update_user_credit_score(user_id)
                if result:
                    results['updated_users'] += 1
                    if result['credit_score'] is not None:
                        results['users_with_scores'] += 1
                    else:
                        results['users_without_history'] += 1
                else:
                    results['failed_users'] += 1
        
        return results 