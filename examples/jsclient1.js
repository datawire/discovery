// JavaScript Hello DWC client, necessarily async

var Discovery = require('daas').Discovery;

// This is our function that makes a call to our service.
function call_service(endpoint, args) {
  // pass
}

/******** Browser event handlers ********/
//
// Assume onClick() is bound to a click or something.

function onClick(event) {
  // Discoball ho.
  var discoball = new Discovery();

  // Where can we find the "my_service" service? As called here, resolve will look
  // up the token for us (from HTML5 localstorage), and return a promise of the service
  // info.

  discoball.resolve("my_service").then(
    function resolved(service_info) {
      // We have a service. Choose an endpoint -- this returns, again, a promise of
      // an endpoint (because choose() may get more complex later).
      return service_info.choose();
    }
  ).then(
    function chosen(endpoint) {
      // Finally, in JavaScript land call_service() will return a promise too.
      return call_service(endpoint, "blah");
    }
  ).then(
    function all_done(data) {
      // update DOM here or whatever.
    },
    function on_error(error) {
      // scream
    }
  );
}
