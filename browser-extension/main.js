/**
 * Code for the popup of the browser extension
 *
 * The popup will provide the following actions:
 * - Start a refinement session
 * - Estimate a new ticket in the current session
 * - Re-estimate the current ticket
 *
 * When opening the popup, the interface should reflect the available actions
 * - Start a refinement session: always
 * - Estimate new ticket: only when there is an active session
 * - Re-estimete the current ticket: only when there is an active session
 */

function storeCurrentSession(data) {
    chrome.storage.local.set({currentSession: data}).then( () => {console.log('Stored refinement data', data)})
}

function getCurrentSession() {
    return chrome.storage.local.get(["currentSession"]).then( (result) => {
	return result
    })
}

function refinementStarted(data) {
    let activityElement = document.getElementById('activity')
    activityElement.innerHTML = ''
    const refinementURL = 'http://localhost:8080' + data['refinement-path']
    const estimationURL = refinementURL + '/estimate'

    navigator.clipboard.writeText(estimationURL)

    const text = document.createTextNode('Estimation link copied to clipboard!')
    activityElement.appendChild (text)

    let button = document.createElement('button')
    button.appendChild (document.createTextNode ('Click here for live updates'))
    button.addEventListener('click', () => {
	chrome.tabs.create({'url': refinementURL})
    })
    activityElement.appendChild (button)
}

function getTicketURLFromCurrentTab() {
    let p = new Promise((resolve) => {
	chrome.tabs.query({active: true, lastFocusedWindow: true}, (tabs) => {
	    resolve(tabs[0].url)
	})
    })

    return p
}

function refineTicket(ticketURL, refinementCode) {

    let url = 'http://127.0.0.1:8080/api/refine'

    if ( refinementCode ) {
	url = `${url}/${refinementCode}`
    }

    fetch(url, {
	method: 'POST',
	headers: {'Content-Type': 'application/json'},
	body: JSON.stringify({'ticket-url': ticketURL})
    }).then((response) => {
	console.log(response)
	return response.json()
    }).then((data) => {
	console.log(data)
	storeCurrentSession(data)
	refinementStarted(data)
    })
}

function setup() {
    let startButton = document.getElementById('startRefinementButton')

    startButton.addEventListener( 'click', () => {
	getTicketURLFromCurrentTab().then( (ticketURL) => {
	    console.log(ticketURL)
	    refineTicket(ticketURL)
	})
    })

    getCurrentSession().then ( (result) => {
	if(result.currentSession) {
	    console.log (result)
	    refinementStarted(result.currentSession)
	}
    })
}

setup()
