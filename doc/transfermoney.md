# Transfer Money Endpoint

**URL** : https://bank.raspicluster.pl/api/account/transferMoney

**Method** : `POST`

**Required Body** :
```json
{
    "fromAccountNumber": "117e7ec8-80b4-4bcf-ba01-10d6f3ef63be",
    "toAccountNumber": "27b803d3-25df-4a36-a838-2482383f8c99",
    "value": 500
}
```

## Success Response

**Condition** : No validation errors

**Code** : `200`
```json
{
    "status": "OK",
    "message": "Money will be transferred",
     "links": [
            {
                "rel": "self",
                "href": "/api/account",
                "method": "get"
            },
            {
                "rel": "self",
                "href": "/api/account",
                "method": "post"
            },
            {
                "rel": "self",
                "href": "/api/account/transferMoney",
                "method": "post"
            }
    ]
}
```

## Error Response

**Condition** : Validation Errors

**Code** : `400`
```json
{
    "status": "ERROR",
    "message": "There are validation errors",
    "data": {
        "fromAccountNumber": [
            "Is not a valid UUID value"
        ],
        "toAccountNumber": [
            "Is not a valid UUID value"
        ],
        "value": [
            "Must be provided & be greater than 0"
        ]
    }
}
```

**Condition** : `fromAccountNumber` is the same as `toAccountNumber`

**Code** : `400`
```json
{
    "status": "ERROR",
    "message": "There are validation errors",
    "data": {
        "toAccountNumber": [
            "Is not possible to transfer money to the same account"
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
