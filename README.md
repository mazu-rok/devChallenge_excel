# Application for [dev challenge](https://devchallenge.it) 

## Result
* Points: 250(384 max)
* Qualified to: Final (threshold score for getting to the final 199)

## Start server
```shell
docker-compose up
```
Server will start on port 8080

## Run tests
```shell
docker-compose -f docker-compose.test.yml up
```

## Description
We all know there is no better software in the world than Excel
The powerful idea behind the cells and formulas allows many of us to understand programming.
Today is your time to pay respect to spreadsheets and implement backend API for this fantastic tool. 

### Description of input data

As a user, I want to have API service with exact endpoints:

#### POST /api/v1/:sheet_id/:cell_id accept params {"value": "1"} implements UPSERT strategy (update or insert) for both sheet_id and cell_id
1) 201 if the value is OK
2) 422 if the value is not OK e.g. new value leads to dependent formula ERROR compilation
   Examples:
   - POST /api/v1/devchallenge-xx/var1 with {"value": "0"}
     - Response: {"value:": "0", "result": "0"}
   - POST /api/v1/devchallenge-xx/var1 with {"value": "1"}
     - Response: {"value:": "1", "result": "1"}
   - POST /api/v1/devchallenge-xx/var2 with {"value": "2"}
     - Response: {"value:": "2", "result": "2"}
   - POST /api/v1/devchallenge-xx/var3 with {"value": "=var1+var2"}
     - Response: {"value": "=var1+var2", "result": "3"}
   - POST /api/v1/devchallenge-xx/var4 with {"value": "=var3+var4"}
     - Response: {"value": "=var3+var4", "result": "ERROR"}

#### GET  /api/v1/:sheet_id/:cell_id
1) 200 if the value present
2) 404 if the value is missing
   Examples:
   - GET /api/v1/devchallenge-xx/var1
     - Response: {"value": "1", result: "1"}
   - GET /api/v1/devchallenge-xx/var1
     - Response: {"value": "2", result: "2"}
   - GET /api/v1/devchallenge-xx/var3
     - Response: {"value": "=var1+var2", result: "3"}

#### GET /api/v1/:sheet_id
1) 200 if the sheet is present
2) 404 if the sheet is missing
   Response:
   ```json
   {
     "var1": {"value": "1", "result": "1"},
     "var2": {"value": "2", "result": "2"},
     "var3": {"value": "=var1+var2", "result": "3"}
   }
   ```

## Requirements
- Supports basic data types: string, integer, float
- Support basic math operations like +, -, /, * and () as well.
- :sheet_id - should be any URL-compatible text that represents the namespace and can be generated on the client
- :cell_id - should be any URL-compatible text that represents a cell (variable) and can be generated on the client
- :sheet_id and :cell_id are case-insensitive
- Be creative and cover as many corner cases as you can. Please do not forget to mention them in the README.md
- Data should be persisted and available between docker containers restarts

## Next steps
- Add more test cases to improve stability
- Improve calculation logic to increase performance