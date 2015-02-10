var app = require('http').createServer(handler)
  , io = require('socket.io').listen(app)
  , fs = require('fs')
  , port = 3000
  , gyroLog = fs.createWriteStream( '' + Date.now() + 'gyroLog.txt');

app.listen(port, function() {
  console.log('Server listening on: ' + app.address().address + ':' + app.address().port);
});

function handler (req, res) {
  console.log('handling a request');
  fs.readFile(__dirname + '/index.html',
  function (err, data) {
    if (err) {
      res.writeHead(500);
      return res.end('Error loading index.html');
    }

    res.writeHead(200);
    res.end(data);
  });
}

io.set('log level', 0);

io.sockets.on('connection', function (socket) {
  socket.emit('news', { hello: 'world' });
  console.log('connected');


  socket.on('accel', function (data) {
    socket.broadcast.emit('accel', data);
//    console.log("accel: ", data);
  });
  
  socket.on('gyro', function (data) {
    socket.broadcast.emit('gyro', data);
    gyroLog.write(JSON.stringify(data));
    gyroLog.write('\n');
    // console.log("gyro: ", data);
  });

  socket.on('select', function (data) {
    socket.broadcast.emit('select', data);
 //   console.log("select: ", data);
  });

  socket.on('tapstart', function (data) {
    socket.broadcast.emit('tapstart', data);
//    console.log("tapstart: ", data);
  });

  socket.on('tapend', function (data) {
    socket.broadcast.emit('tapend', data);
//    console.log("tapend: ", data);
  });

  socket.on('tapmove', function (data) {
    socket.broadcast.emit('tapmove', data);
//    console.log("tapmove: ", data);
  });

  socket.on('record', function(data) {
    gyroLog = fs.createWriteStream( '' + Date.now() + 'gyroLog.txt');
  });

});
