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

const config = { baseURL: 'http://localhost:8080' }

function storeCurrentSession(data) {
    chrome.storage.local.set({currentSession: data}).then( () => {
	console.log('Stored refinement data', data)
    })
}

function getCurrentSession() {
    return chrome.storage.local.get(["currentSession"]).then( (result) => {
	return result
    })
}

function refinementStarted(data) {
    const refinementURL = config.baseURL + data['refinement-path']
    const estimationURL = refinementURL + '/estimate'

    navigator.clipboard.writeText(estimationURL)

    let activityElement = document.getElementById('activity')
    activityElement.innerHTML = ''

    var p = document.createElement('p')
    p.appendChild(document.createTextNode('Current session\'s estimation link copied to clipboard.'))
    activityElement.appendChild(p)

    p = document.createElement('p')
    let tabButton = document.createElement('button')
    tabButton.appendChild(document.createTextNode('Click here for live updates'))
    tabButton.addEventListener('click', () => {
	chrome.tabs.create({'url': refinementURL})
    })
    p.appendChild(tabButton)
    activityElement.appendChild(p)

    p = document.createElement('p')
    let reestimateButton = document.createElement('button')
    reestimateButton.appendChild(document.createTextNode('Re estimate'))
    reestimateButton.addEventListener('click', () => {
        reestimateTicket(data['ticket-id'], data['refinement-code'])
    })
    p.appendChild(reestimateButton)
    activityElement.appendChild(p)
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

    let url = `${config.baseURL}/api/refine`

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

function reestimateTicket(ticketId, refinementCode) {

    let url = `${config.baseURL}/api/refine/${refinementCode}/ticket/${ticketId}/re-estimate`

    fetch(url, {
	method: 'POST',
	headers: {'Content-Type': 'application/json'}
    }).then((response) => {
	console.log("Re-estimation successful")
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