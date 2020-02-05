# Create Account Endpoint

**URL** : https://bank.raspicluster.pl/api/account

**Method** : `POST`

**Required Body** :
```json
{
    "fullName": "Tony Stark",
}
```

## Success Response

**Condition** : Account is created

**Code** : `201`
```json
{
    "status": "OK",
    "message": "Account will be created",
    "data": "f0687dab-e497-44f9-b9c3-42afe4e420cf",
    "links": [
            {
                "rel": "self",
                "href": "/api/account/f0687dab-e497-44f9-b9c3-42afe4e420cf",
                "method": "get"
            },
            {
                "rel": "self",
                "href": "/api/account/f0687dab-e497-44f9-b9c3-42afe4e420cf/changeFullName",
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
