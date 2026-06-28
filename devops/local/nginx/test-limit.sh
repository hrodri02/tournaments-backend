#!/bin/bash
seq 30 | xargs -P 30 -I {} curl -s -o /dev/null -w "Request {}: %{http_code}\n" \
  http://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'
