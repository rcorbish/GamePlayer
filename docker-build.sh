#!/bin/sh

docker -H tcp://mercury.rac.local:2375 build -t rcorbish/game-player .

