# User Service API Test Calls

## Base URL
```
http://localhost:8080/api/v1/users
```

## Authentication
All admin endpoints require a valid JWT token with SuperAdmins authority.
Add to headers:
```
Authorization: Bearer <your-jwt-token>
```

---

## 1. Search Users (Enhanced Multi-Parameter Search)

### Endpoint
```
GET /api/v1/users/admin/search
```

### Test Cases

#### 1.1 Search by Email
```bash
curl -X GET "http://localhost:8080/api/v1/users/admin/search?email=john" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

#### 1.2 Search by First Name
```bash
curl -X GET "http://localhost:8080/api/v1/users/admin/search?firstName=John" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

#### 1.3 Search by Last Name
```bash
curl -X GET "http://localhost:8080/api/v1/users/admin/search?lastName=Doe" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

#### 1.4 Search by Username
```bash
curl -X GET "http://localhost:8080/api/v1/users/admin/search?username=johndoe" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

#### 1.5 Search by Status
Valid status values: `CONFIRMED`, `FORCE_CHANGE_PASSWORD`, `UNCONFIRMED`, etc.
```bash
curl -X GET "http://localhost:8080/api/v1/users/admin/search?status=CONFIRMED" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

#### 1.6 Search by Role (User Group)
Valid roles: `SuperAdmins`, `Suppliers`, `DataStewards`, `Customers`
```bash
curl -X GET "http://localhost:8080/api/v1/users/admin/search?role=Suppliers" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

#### 1.7 Combined Search (Multiple Parameters)
```bash
curl -X GET "http://localhost:8080/api/v1/users/admin/search?firstName=John&role=Suppliers&status=CONFIRMED" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

#### 1.8 Advanced Combined Search
```bash
curl -X GET "http://localhost:8080/api/v1/users/admin/search?email=example.com&firstName=John&lastName=Doe&status=CONFIRMED" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

#### 1.9 Search All Users (No Parameters)
```bash
curl -X GET "http://localhost:8080/api/v1/users/admin/search" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

### Expected Response
```json
{
  "success": true,
  "message": "Search completed successfully",
  "data": [
    {
      "id": "user-sub-id",
      "username": "johndoe@example.com",
      "email": "johndoe@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "phone": "+1234567890",
      "emailVerified": true,
      "status": "CONFIRMED",
      "createdDate": "2024-01-15T10:30:00Z",
      "lastModifiedDate": "2024-10-20T14:22:00Z",
      "userGroups": ["Suppliers"]
    }
  ],
  "timestamp": "2024-10-28T12:00:00Z"
}
```

---

## 2. Get All Users (Paginated)

### Endpoint
```
GET /api/v1/users/admin
```

### Test Cases

#### 2.1 Get First Page (Default limit: 20)
```bash
curl -X GET "http://localhost:8080/api/v1/users/admin" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

#### 2.2 Get with Custom Limit
```bash
curl -X GET "http://localhost:8080/api/v1/users/admin?limit=10" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

#### 2.3 Get Next Page
```bash
curl -X GET "http://localhost:8080/api/v1/users/admin?limit=20&nextToken=<token-from-previous-response>" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

### Expected Response
```json
{
  "success": true,
  "message": "Users retrieved successfully",
  "data": {
    "users": [
      {
        "id": "user-sub-id",
        "username": "user@example.com",
        "email": "user@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "phone": "+1234567890",
        "emailVerified": true,
        "status": "CONFIRMED",
        "createdDate": "2024-01-15T10:30:00Z",
        "lastModifiedDate": "2024-10-20T14:22:00Z",
        "userGroups": ["Customers"]
      }
    ],
    "nextToken": "pagination-token-or-null"
  },
  "timestamp": "2024-10-28T12:00:00Z"
}
```

---

## 3. Get User by ID

### Endpoint
```
GET /api/v1/users/admin/{userId}
```

### Test Case
```bash
curl -X GET "http://localhost:8080/api/v1/users/admin/abc123-user-sub-id" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json"
```

---

## 4. Create Admin User

### Endpoint
```
POST /api/v1/users/admin
```

### Test Case
```bash
curl -X POST "http://localhost:8080/api/v1/users/admin" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newsupplier@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "role": "Suppliers"
  }'
```

---

## 5. Update User Roles

### Endpoint
```
PUT /api/v1/users/admin/{userId}/role
```

### Test Case
```bash
curl -X PUT "http://localhost:8080/api/v1/users/admin/abc123-user-sub-id/role" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "roles": ["Suppliers", "DataStewards"]
  }'
```

---

## 6. Update User Status

### Endpoint
```
PUT /api/v1/users/admin/{userId}/status
```

#### 6.1 Enable User
```bash
curl -X PUT "http://localhost:8080/api/v1/users/admin/abc123-user-sub-id/status" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true
  }'
```

#### 6.2 Disable User
```bash
curl -X PUT "http://localhost:8080/api/v1/users/admin/abc123-user-sub-id/status" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": false
  }'
```

---

## Postman Collection Format

### Search Users - Multiple Parameters
```json
{
  "name": "Search Users",
  "request": {
    "method": "GET",
    "header": [
      {
        "key": "Authorization",
        "value": "Bearer {{jwt_token}}",
        "type": "text"
      }
    ],
    "url": {
      "raw": "{{base_url}}/api/v1/users/admin/search?email=&firstName=&lastName=&username=&status=&role=",
      "host": ["{{base_url}}"],
      "path": ["api", "v1", "users", "admin", "search"],
      "query": [
        {
          "key": "email",
          "value": "",
          "description": "Filter by email (partial match)"
        },
        {
          "key": "firstName",
          "value": "",
          "description": "Filter by first name (partial match)"
        },
        {
          "key": "lastName",
          "value": "",
          "description": "Filter by last name (partial match)"
        },
        {
          "key": "username",
          "value": "",
          "description": "Filter by username (partial match)"
        },
        {
          "key": "status",
          "value": "",
          "description": "Filter by status (exact match)"
        },
        {
          "key": "role",
          "value": "",
          "description": "Filter by role/group (exact match)"
        }
      ]
    }
  }
}
```

---

## Testing with JavaScript/Fetch

```javascript
// Search users by multiple parameters
async function searchUsers(params) {
  const queryParams = new URLSearchParams();
  
  if (params.email) queryParams.append('email', params.email);
  if (params.firstName) queryParams.append('firstName', params.firstName);
  if (params.lastName) queryParams.append('lastName', params.lastName);
  if (params.username) queryParams.append('username', params.username);
  if (params.status) queryParams.append('status', params.status);
  if (params.role) queryParams.append('role', params.role);
  
  const response = await fetch(
    `http://localhost:8080/api/v1/users/admin/search?${queryParams}`,
    {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      }
    }
  );
  
  return await response.json();
}

// Example usage
searchUsers({
  firstName: 'John',
  role: 'Suppliers',
  status: 'CONFIRMED'
}).then(data => console.log(data));
```

---

## Query Parameter Details

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| email | String | No | Partial match on email address | `john@example` |
| firstName | String | No | Partial match on first name | `John` |
| lastName | String | No | Partial match on last name | `Doe` |
| username | String | No | Partial match on username | `johndoe` |
| status | String | No | Exact match on user status | `CONFIRMED` |
| role | String | No | Exact match on user group/role | `Suppliers` |

### Notes:
- All parameters are optional
- Multiple parameters can be combined (AND logic)
- Email, firstName, lastName, and username use **case-insensitive partial matching**
- Status and role use **exact matching**
- If no parameters are provided, all users are returned

