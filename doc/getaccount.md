# Get Account Endpoint

**URL** : `/api/account/getAccount/:uuid`

**Method** : `GET`

**Required Body** : `{}`

## Success Response

**Condition** : Account is found

**Code** : `200`
```json
{
    "status": "OK",
    "message": "SUCCESS",
    "data": {
        "fullName": "John Doe",
        "accountNumber": "5c6c3d72-2be9-4bcd-aac8-15cd4dd58ba1",
        "balance": 1000,
        "transactionToReservedBalance": {},
        "events": [
            {
                "fullName": "John Doe",
                "eventType": "ACCOUNT_CREATED_EVENT",
                "aggregateUUID": "5c6c3d72-2be9-4bcd-aac8-15cd4dd58ba1",
                "createdAt": "Jan 4, 2019 12:35:54 PM"
            }
        ],
        "transactions": {},
        "createdAt": "Jan 4, 2019 12:35:54 PM",
        "lastUpdatedAt": "Jan 4, 2019 12:35:54 PM"
    }
}
```

## Error Response

**Condition** : UUID in path is not valid

**Code** : `400`
```json
{
    "status": "ERROR",
    "message": "There are validation errors",
    "data": {
        "uuid": [
            "Is not a valid UUID value"
        ]
    }
}
```

**Condition** : Account is not found

**Code** : `404`
```json
{
    "status": "ERROR",
    "message": "Account with ID: 5c6c3d72-2be9-4bcd-aac8-15cd4dd58ba5 was not found"
}
```