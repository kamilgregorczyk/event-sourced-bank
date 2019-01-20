# Change Full Name Endpoint

**URL** : `/api/account/:UUID/changeFullName`

**Method** : `PUT`

**Required Body** : 
```json
{
    "fullName": "Tony Stark",
}
```

## Success Response

**Condition** : Account is found

**Code** : `200`
```json
{
    "status": "OK",
    "message": "Full Name will be changed",
    "links": [
            {
                "rel": "self",
                "href": "/api/account/5074d966-8887-4453-ac98-62755f426c64",
                "method": "get"
            },
            {
                "rel": "self",
                "href": "/api/account/5074d966-8887-4453-ac98-62755f426c64/changeFullName",
                "method": "put"
            }
  ]
}
```

## Error Response

**Condition** : Empty `fullName`

**Code** : `400`
```json
{
    "status": "ERROR",
    "message": "There are validation errors",
    "data": {
        "fullName": [
            "Cannot be empty"
        ]
    }
}
```

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
