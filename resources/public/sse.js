function connect_to_events (url, message_handler){
  var source = new EventSource(url)

  source.onmessage = (e) => {
    console.log(e)

    message_handler(e)
  }

  source.onopen = (e) => {
    console.log('connection opened:' + e)
  }

  source.onerror = (e) => {
    console.log('error:' + e)
    console.log(e)
    if (e.readyState == EventSource.CLOSED) {
      console.log('connection closed:' + e)
    }
    source.close()
  }

  return source
}
