#!/bin/bash
# starts mail server in the background
maildev &
# starts postgresql server 
sudo systemctl start postgresql
# starts tournaments backend app in the background
java -jar target/tournaments-backend-0.0.1-SNAPSHOT.jar &