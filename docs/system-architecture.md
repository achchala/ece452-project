flowchart TB
    %% Styling
    classDef android fill:#e1f5fe,stroke:#039be5,color:#333
    classDef django fill:#e8f5e9,stroke:#43a047,color:#333
    classDef firebase fill:#ffecb3,stroke:#ffa000,color:#333
    classDef db fill:#bbdefb,stroke:#1976d2,color:#333
    classDef note fill:#f3e5f5,stroke:#7b1fa2,color:#333
    
    %% Android Client
    subgraph Android["Android Client (Kotlin)"]
        UI[UI Components<br/>Jetpack Compose]:::android
        VM[ViewModel<br/>Android Architecture]:::android
        FA[Firebase Auth SDK<br/>Google]:::android
        NotifClient[Notification Handler<br/>FCM Client]:::android
    end
    
    %% Django Backend
    subgraph Django["Django Backend (Monolith)"]
        API[API Views<br/>Django REST]:::django
        
        subgraph Auth["Auth Module"]
            FASDK[Firebase Admin SDK<br/>Google]:::firebase
            JWT[JWT Token Handler]:::django
        end
        
        subgraph Logic["Business Logic"]
            User[User Service<br/>Profile & Friends]:::django
            Group[Group Service<br/>Management & Budgets]:::django
            Expense[Expense Service<br/>Split Logic & Settlement]:::django
            Analytics[Analytics Service<br/>Credit Score & Dashboard]:::django
        end
        
        subgraph Notifications["Notification System"]
            NotifService[Notification Service]:::django
            FCMServer[FCM Server SDK<br/>Google]:::firebase
        end
        
        DB[(PostgreSQL<br/>Database)]:::db
    end
    
    %% Firebase Services (External)
    Firebase[[Firebase Auth<br/>External]]:::firebase
    FCM[[Firebase Cloud Messaging<br/>External]]:::firebase
    
    %% Connections
    UI -->|State updates| VM
    VM -->|Sign-in with Google| FA
    FA -->|ID Token| Firebase
    Firebase -->|User Credentials| FA
    VM -->|Authenticated Requests<br/>Headers: JWT| API
    API -->|Verify ID Token| FASDK
    FASDK -->|Decoded Claims| JWT
    JWT -->|User Context| API
    
    %% Business Logic Connections
    API -->|Internal Calls| User
    API -->|Internal Calls| Group
    API -->|Internal Calls| Expense
    API -->|Internal Calls| Analytics
    Expense -->|Calculate Balances| Analytics
    Group -->|Budget Alerts| NotifService
    Expense -->|New Expense/Settlement| NotifService
    
    %% Database Connections
    User -->|CRUD Users/Friends| DB
    Group -->|CRUD Groups/Budgets| DB
    Expense -->|CRUD Expenses/Balances| DB
    Analytics -->|Read/Update Scores & Dashboards| DB
    
    %% Notification Flow
    NotifService -->|Send Push| FCMServer
    FCMServer -->|Push Notification| FCM
    FCM -->|Deliver to Device| NotifClient
    
    %% Notes
    note1["Firebase Flow:<br/>1. Android sends Google ID token<br/>2. Django verifies via Firebase Admin SDK<br/>3. Returns Django-signed JWT"]:::note
    
    note2["Database Schema:<br/>• users: firebase_uid, email, username, profile_pic<br/>• friends: user relationships (M2M)<br/>• groups: name, members (M2M)<br/>• expenses: title, splits (JSON), participants<br/>• balances: net_amounts per user/group<br/>• budgets: limits, periods, progress<br/>• credit_scores: calculated scores & history"]:::note
    
    note3["Split Logic:<br/>• Even split: amount ÷ participants<br/>• Exact amounts: custom per person<br/>• Percentage split: % allocation<br/>• Settle up: net balance calculation"]:::note
    
    FASDK -.-> note1
    DB -.-> note2
    Settle -.-> note3
