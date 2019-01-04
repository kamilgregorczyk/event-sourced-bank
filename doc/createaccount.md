# Create Account Endpoint

**URL** : `/api/account/createAccount`

**Method** : `POST`

**Required Body** : `{"fullName": "Kamil Gregorczyk"}`

## Success Response

**Condition** : Account is created

**Code** : `201`
```
{
    "status": "OK",
    "message": "Account will be created",
    "data": "f0687dab-e497-44f9-b9c3-42afe4e420cf"
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
