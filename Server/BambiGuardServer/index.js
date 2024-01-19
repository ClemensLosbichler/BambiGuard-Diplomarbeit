const express = require("express"),
  http = require("http"),
  app = express(),
  server = http.createServer(app),
  io = require("socket.io")().listen(server);

var waitingHelpers = [];
var sessions = [];

class User {
  static progressiveId = 0;

  constructor(name, type, socket) {
    this.name = name;
    this.type = type;
    this.socket = socket;
    this.id = User.progressiveId;
    User.progressiveId++;
  }

  toString() {
    return this.type + ": " + this.name;
  }
}

class Session {
  constructor(pilot) {
    this.addPilot(pilot);
    this.helpers = [];
  }

  addPilot(pilot) {
    this.pilot = pilot;

    this.pilot.socket.on("get_helpers", (...args) => {
      waitingHelpers.forEach((helper) => {
        console.log("send helper " + helper);
        this.pilot.socket.emit("helper", helper.name, helper.id);
      });
    });

    this.setOnPilotNameChanged();
    this.setOnBambiFound();
    this.setOnHelpersAquired();
  }

  setOnPilotNameChanged() {
    this.pilot.socket.on("pilot_name_changed", (...args) => {
      console.log("pilot_name_changed " + args);
      this.pilot.name = args[0];
      this.emitToAllHelpers("pilot_name_changed", this.pilot.name);
    });
  }

  setOnBambiFound() {
    this.pilot.socket.on("bambi_found", (...args) => {
      console.log("bambi_found " + args);
      this.emitToAllHelpers("bambi_found", args);
    });
  }

  setOnHelpersAquired() {
    this.pilot.socket.on("acquire_helper", (...args) => {
      console.log("acquire_helper " + args);

      let id = args[0];
      let helper = waitingHelpers.at(id);
      this.addHelper(helper);

      waitingHelpers = waitingHelpers.slice(id, id + 1);
      waitingHelpers.remove(id);
    });
  }

  addHelper(helper) {
    this.helpers.push(helper);

    helper.socket.emit("join_flight", this.pilot.name);
    helper.socket.on("bambi_rescued", (...args) => {
      console.log("bambi_rescued " + args);
      this.pilot.socket.emit("bambi_rescued", args);
    });
  }

  pilotDisconnected() {
    this.emitToAllHelpers("pilot_disconnected", "");
  }

  emitToAllHelpers(event, args) {
    this.helpers.forEach((helper) => {
      helper.socket.emit(event, args);
    });
  }
}

function testHelper(helper) {
  helper.socket.emit("join_flight", "Felix");
}

io.on("connection", (socket) => {
  console.log("user connected");
  let status = "swimming";

  socket.on("user_identification", (...args) => {
    let user = new User(args[1], args[0], socket);

    console.log("user identification " + user.toString());

    if (user.type == "pilot") {
      let session = new Session(user);
      sessions.push(session);
      status = session;
    } else if (user.type == "helper") {
      waitingHelpers.push(user);
      status = user;
      testHelper(user);
    }
  });

  socket.on("disconnect", (reason) => {
    console.log("user disconnected " + reason);

    if (status instanceof Session) {
      status.pilotDisconnected();
    } else if (status instanceof User) {
      let index = waitingHelpers.indexOf(status);
      if (index != -1) waitingHelpers.splice(index, 1);
    }
  });
});

app.get("/", (req, res) => {
  res.send("BambiGuard Server is running on port 3000");
});

server.listen(3000, () => {
  console.log("BambiGuard Server is running on port 3000");
});
