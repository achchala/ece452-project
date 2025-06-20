import pytest
from unittest.mock import MagicMock, patch
from rest_framework.test import APIRequestFactory
from rest_framework import status
from core.views.auth import AuthView

@pytest.fixture
def api_request_factory():
    return APIRequestFactory()

@patch('core.views.auth.supabase')
def test_register_user_success(mock_supabase, api_request_factory):
    # Arrange
    view = AuthView.as_view({'post': 'register'})
    mock_supabase.users.get_by_firebase_id.return_value = None
    mock_supabase.users.create.return_value = {
        'id': 'some-uuid',
        'email': 'test@example.com',
        'firebase_id': 'test_firebase_id'
    }

    data = {
        'email': 'test@example.com',
        'firebaseId': 'test_firebase_id'
    }
    request = api_request_factory.post('/api/register/', data)

    # Act
    response = view(request)

    # Assert
    assert response.status_code == status.HTTP_201_CREATED
    assert response.data['message'] == 'User created successfully'
    assert response.data['user']['email'] == 'test@example.com'
    mock_supabase.users.get_by_firebase_id.assert_called_once_with('test_firebase_id')
    mock_supabase.users.create.assert_called_once_with(
        email='test@example.com', firebase_id='test_firebase_id'
    )

@patch('core.views.auth.supabase')
def test_register_user_already_exists(mock_supabase, api_request_factory):
    # Arrange
    view = AuthView.as_view({'post': 'register'})
    mock_supabase.users.get_by_firebase_id.return_value = {
        'id': 'some-uuid',
        'email': 'test@example.com',
        'firebase_id': 'test_firebase_id'
    }
    
    data = {
        'email': 'test@example.com',
        'firebaseId': 'test_firebase_id'
    }
    request = api_request_factory.post('/api/register/', data)

    # Act
    response = view(request)

    # Assert
    assert response.status_code == status.HTTP_409_CONFLICT
    assert response.data['error'] == 'User with this Firebase ID already exists'
    mock_supabase.users.get_by_firebase_id.assert_called_once_with('test_firebase_id')
    mock_supabase.users.create.assert_not_called()

def test_register_user_missing_fields(api_request_factory):
    # Arrange
    view = AuthView.as_view({'post': 'register'})
    data = {'email': 'test@example.com'}  # Missing firebaseId
    request = api_request_factory.post('/api/register/', data)

    # Act
    response = view(request)

    # Assert
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.data['error'] == 'Email and firebaseId are required' 