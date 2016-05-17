use ./quark-improvements.q;

import quark.concurrent;
import quark_ext1;

class Maybe extends nice.Future {
	int data;
  String error;
}

class Check extends nice.Bindable {
  static Logger logger = new Logger("Check");

  Maybe maybeCheck() {
    Maybe result = new Maybe();

    logger.info("got new Maybe");

    result.then(self.__method__("onResolution"), [ "blah" ]);

    logger.info("returning new Maybe");
    return result;
  }

  void onResolution(Maybe m, String what) {
    logger.info("onResolution: " + what + ", " + m.data.toString());
  }
}
