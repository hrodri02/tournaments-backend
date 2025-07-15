#!/bin/bash
# stop the spring app
ID=$(sudo lsof -i:8080 | sed -n '2p' | cut -d' ' -f2)
sudo kill -9 $ID
# stop the database
ID=$(sudo lsof -i:5432 | sed -n '2p' | cut -d' ' -f2)
sudo kill -9 $ID
# stop the mail server
ID=$(sudo lsof -i:1080 | sed -n '2p' | cut -d' ' -f2)
sudo kill -9 $ID