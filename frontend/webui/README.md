Frontend Web UI React app

Runs API as well, since without it this is literally just a static file server

Development:
* Install npm dependencies with ```npm install```
* Run in dev mode with ```sbt run```
* To automatically reload ui sources (the React part) ```npm run dev``` needs to be running alongside this

Production:
* To create a distributable package use ```sbt webui/dist```    
this creates a zip in frontend/webui/target/universal that contains the whole distribution
* To launch the package just unpack it and run ```bin/webui -Dplay.http.secret.key=[secret key]```

(for more info on running play apps in production - https://www.playframework.com/documentation/2.8.x/Deploying)

using react with play stolen from https://github.com/maximebourreau/play-reactjs-es6-seed
