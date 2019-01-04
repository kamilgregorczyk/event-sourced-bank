# Change Full Name Endpoint

**URL** : `/api/account/changeFullName/:uuid`

**Method** : `POST`

**Required Body** : `{"fullName": "Kamil Gregorczyk"}`

## Success Response

**Condition** : Account is found

**Code** : `200`
```
{
    "status": "OK",
    "message": "Full Name will be changed"
}
```

## Error Response

**Condition** : Empty `fullName`

**Code** : `400`
```
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
```
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
```
{
    "status": "ERROR",
    "message": "Account with ID: 5c6c3d72-2be9-4bcd-aac8-15cd4dd58ba5 was not found"
}
```
