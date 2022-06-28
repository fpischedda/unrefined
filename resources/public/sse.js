function connect_to_events(code){
  var source = new EventSource('/events/' + code);

  source.onmessage = (e) => {
    console.log(e);
    // const data = JSON.parse(e.data);
    // console.log(data);

    // if (data.msg == "end") {
    //   console.log("Closing the stream.");
    //   source.close();
    // }
    //update time series graph, tabular data etc
  };

  source.onopen = (e) => {
    console.log("connection opened:" + e);
  };

  source.onerror = (e) => {
    console.log("error:" + e);
    console.log(e);
    if (e.readyState == EventSource.CLOSED) {
      console.log("connection closed:" + e);
    }
    source.close();
  };

  return source;
}
