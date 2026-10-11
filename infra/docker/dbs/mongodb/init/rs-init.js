try {
  rs.status();
  print("replica set already initiated");
} catch (e) {
  print("initiating replica set rs0...");
  rs.initiate({
    _id: "rs0",
    members: [
      { _id: 0, host: "mongo1:27117", priority: 2 },
      { _id: 1, host: "mongo2:27118", priority: 1 },
      { _id: 2, host: "mongo3:27119", priority: 1 }
    ]
  });
}

let tries = 0;
while (!db.hello().isWritablePrimary && tries < 60) {
  sleep(1000);
  tries++;
}
printjson(rs.status().members.map(m => m.name + " " + m.stateStr));
