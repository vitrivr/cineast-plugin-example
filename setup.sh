#!/bin/bash

#This script initializes the code dependencies to Cineast and starts an initial gradle build to initialize gradle dependencies as well.

git clone https://github.com/vitrivr/cineast.git cineast
./gradlew build