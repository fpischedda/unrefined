function refinementStarted(data) {
   let activityElement = document.getElementById('activity')
   const refinementURL = 'http://localhost:8080' + data['refinement-path']
   const estimationURL = refinementURL + '/estimate'

   navigator.clipboard.writeText(estimationURL)
   activityElement.innerHTML = `Estimation link copied to clipboard!<br/>Click <a href="${refinementURL}">here</a> for live updates.`
}

function getTicketURLFromCurrentTab(callback) {
   chrome.tabs.query({active: true, lastFocusedWindow: true}, (tabs) => {
      callback(tabs[0].url) })
}

function refineTicket(ticketURL) {
   console.log("ticket url: " + ticketURL) 

   fetch('http://127.0.0.1:8080/api/refine', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({'ticket-url': ticketURL})
   }).then((response) => {
      console.log(response)
      return response.json()
   }).then((data) => {
      console.log(data)
      refinementStarted(data)
   })
}

function setup() {
   let startButton = document.getElementById('startRefinementButton')

   startButton.addEventListener( 'click', () => { getTicketURLFromCurrentTab(refineTicket) })
}

setup()
