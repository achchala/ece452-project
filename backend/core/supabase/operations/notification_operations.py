from typing import Optional, Dict, List
from ..base_client import BaseSupabaseClient


class NotificationOperations:
    """Handles all notifications-related database operations using the Supabase client."""
    
    def __init__(self, base_client: BaseSupabaseClient):
        self.client = base_client
        self.notification_table = self.client.get_table_name("notification")
    
    def insert_notification(self, user_id: str, notification_message: str, processed: bool) -> Optional[Dict]:
        """Insert a new notification."""
        data = {
            "user_id": user_id,
            "notification_message": notification_message,
            "processed": processed
        }
        
        notification = self.client._execute_query(
            table_name=self.notification_table,
            operation='insert',
            data=data
        )
        
        return notification
    
    def get_all_unprocessed_notifications(self, user_id: str) -> Optional[List[Dict]]: 
        '''Get all unprocessed notifications for given user'''

        notifications = self.client._execute_query(
            table_name=self.notification_table,
            operation='select',
            filters={'user_id': user_id, 'processed': False}
        )

        return notifications
    
    def update_notification_processed(self, notification_id: str) -> Optional[Dict]:
        '''Update a notification to processed'''
        
        notification = self.client._execute_query(
            table_name=self.notification_table,
            operation='update',
            filters={'notification_id': notification_id},
            data={'processed': True}
        )   

        return notification