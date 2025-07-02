# TODO-java

# Simple TODO REST API (Java)

A TODO application built with **pure Java** using `HttpServer`.  
Supports basic CRUD operations with **in-memory storage** and **soft delete**.


## Features

- List TODOs (`GET /todos`)
- Create TODO (`POST /todos`)
- Get TODO by ID (`GET /todos/{id}`)
- Update TODO (`PUT /todos/{id}`)
- Delete TODO (`DELETE /todos/{id}`)


## How to Run

```bash
javac TodoApp.java
java TodoApp
```

server will start at:
http://localhost:8000


Use curl commands for testing:

Get Todo List:
```bash
curl http://localhost:8000/todos
```

Get Todo (by id):
```bash
curl http://localhost:8000/todos/006dbc21-1944-4fb7-bc88-ee5355565e06
```

Post Todos:
```bash
curl -X POST http://localhost:8000/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy milk","description":"Grocery store","completed":false}'
```

Put Todos:
```bash
curl -X POST http://localhost:8000/todos/006dbc21-1944-4fb7-bc88-ee5355565e06 \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy milk","description":"Grocery store","completed":true}'
```

Delete Todo (soft delete):
```bash
curl -X DELETE http://localhost:8000/todos/006dbc21-1944-4fb7-bc88-ee5355565e06
```
