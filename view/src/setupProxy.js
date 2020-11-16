const { createProxyMiddleware } = require('http-proxy-middleware');
const cors = require('cors');

module.exports = function(app) {
  var corsOptions = {
    origin: 'https://localhost:8438',
    credentials: true,
    optionsSuccessStatus: 200 // some legacy browsers (IE11, various SmartTVs) choke on 204
  }
  app.use(cors(corsOptions));
  app.use(createProxyMiddleware('/services', { target: 'https://localhost:8438', secure: false }));
};
