function connect_to_events (url, message_handler,
                            retry = {delay_ms: 1000, max_retries: 0, retries: 0}){

  let source = new EventSource(url)

  source.onmessage = (e) => {
    console.log(e)

    message_handler(e)
  }

  source.onopen = (e) => {
    console.log(`[EventSource] Connection opened to url ${url}`)
    console.log(e)
    retry.retries = 0  // on successful connection reset the retry counter
  }

  source.onerror = (e) => {
    console.log(`[EventSource] Connection error, url ${url}`)
    console.log(e)
    if ( e.target.readyState == EventSource.CLOSED ) {
      console.log('[EventSource] Connection closed:' + e)

      if ( retry.max_retries > 0 && retry.retries < retry.max_retries ) {
        retry.retries += 1
        const delay = retry.delay_ms * retry.retries
        console.log(`[EventSource] Reconnecting to ${url} in ${delay} milliseconds, retries left ${retry.max_retries - retry.retries}`)
        setTimeout(connect_to_events, delay, url, message_handler, retry)
      }
      else {
        console.log('[EventSource] Not trying to reconnect')
      }
    }
    source.close()
  }

  return source
}
