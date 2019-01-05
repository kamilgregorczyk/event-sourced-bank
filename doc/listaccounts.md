# List Accounts Endpoint

**URL** : `/api/account/listAccounts`

**Method** : `GET`

**Required Body** : `{}`

## Success Response

**Condition** : Only one account is registered

**Code** : `200`
```json
{
    "status": "OK",
    "message": "SUCCESS",
    "data": [
        {
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
    ]
}
```

**Condition** : No accounts are registered

**Code** : `200`
```json
{
    "status": "OK",
    "message": "SUCCESS",
    "data": []
}
```
