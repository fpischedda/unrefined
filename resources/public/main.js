function get_refinement_code () {
  const code = document.getElementsByTagName('body')[0].dataset['refinement']
  console.log('session code: ' + code)
  return code
}

function get_ticket_id () {
  const ticket = document.getElementsByTagName('body')[0].dataset['ticket']
  console.log('ticket name: ' + ticket)
  return ticket
}

function get_ticket_path (code, ticket_id) {
  return `/refine/${code}/ticket/${ticket_id}`
}

function get_ticket_estimation_path (code, ticket_id) {
  return get_ticket_path(code, ticket_id) + '/estimate'
}

function get_ticket_results_path (code, ticket_id) {
  return get_ticket_path(code, ticket_id) + '/results'
}

function get_ticket_events_path (code, ticket_id) {
  return get_ticket_path(code, ticket_id) + '/events'
}

function copy_estimation_link (code, ticket_id) {
  var url = document.location.origin + get_ticket_estimation_path(code, ticket_id)
  navigator.clipboard.writeText(url)
}

function goto_estimation_page (code, ticket_id) {
  document.location.href = get_ticket_estimation_path(code, ticket_id)
}

function goto_watch_page (code, ticket_id) {
  document.location.href = get_ticket_path(code, ticket_id)
}

function update_vote_stats (payload) {

  var total_voted = document.getElementById('total-voted')
  if(total_voted == null) return  // not in result page

  total_voted.textContent = payload.voted
  document.getElementById('total-skipped').textContent = payload.skipped

  var html = '<ul>'
  const heart = '\u{2665}'

  payload.votes.forEach(i => {
    const votes = heart.repeat(i.count)
    html += `<li >${i.points} Story points ${votes}</li>`
  })

  html += '</ul>'

  document.getElementById('vote-chart').innerHTML = html
}

function handle_sse_messages (e) {
  const data = JSON.parse(e.data)
  console.log(data)

  if( data.event == 'user-voted' || data.event == 'user-skipped' || data.event == 'ticket-status') {
    update_vote_stats(data.payload)
  }
  else if( data.event == 're-estimate-ticket' && document.location.href.includes('estimate')) {
    goto_estimation_page(data.payload.code, data.payload.ticket_id)
  }
  else if( data.event == 'added-ticket') {
    if(data.payload.ticket_id != get_ticket_id()) {
      if(document.location.href.indexOf('/estimate') >= 0) {
        goto_estimation_page(data.payload.code, data.payload.ticket_id)
      }
      else {
        goto_watch_page(data.payload.code, data.payload.ticket_id)
      }
    }
  }
}

function init_sse () {
  const code = get_refinement_code()
  const ticket_id = get_ticket_id()
  const url = get_ticket_events_path(code, ticket_id)

  console.log(`connecting to SSE endpoint ${url}`)
  connect_to_events(url, handle_sse_messages, {delay_ms: 1000, max_retries: 5, retries: 0})
}

function start () {
  init_sse()
}

function update_total () {
  const breakdowns = [...document.querySelectorAll('.topicRow input')]

  total = 0

  breakdowns.forEach( b => {
    total += parseInt(b.value || 0)
  })

  document.getElementById('points').value = total

  document.getElementById('total-story-points').innerText = total
}

const loadEstimationTopics = (estimationSet) => {

  return fetch(`/assets/estimation-cheatsheets/${estimationSet}.json`)
    .then( (response) => { return response.json()})
    .then( (data) => { return data.estimationTopics})
}

const renderEstimationTopic = (topic, estimationTable, inputCallback) => {
  const newRow = document.createElement('tr')
  newRow.classList.add('topicRow')
  const labelColumn = document.createElement('td')
  const inputColumn = document.createElement('td')
  const exampleColumn = document.createElement('td')
  const labelContent = document.createElement('div')
  labelContent.classList.add('d-flex', 'flex-column')
  labelContent.innerHTML = `<span class="fw-bold">${topic.label}</span><span>Current: <span class="rowEstimate">0</span></span>`
  labelColumn.appendChild(labelContent)
  labelColumn.style.width = '33%'
  inputColumn.style.width = '33%'
  exampleColumn.style.width = '33%'
  const rangeInput = document.createElement('input')
  rangeInput.type = 'range'
  rangeInput.classList.add('form-range')
  rangeInput.value = 0
  rangeInput.min = 0
  rangeInput.max = topic.examples.length
  rangeInput.name = topic.name
  rangeInput.oninput = inputCallback
  inputColumn.appendChild(rangeInput)
  newRow.appendChild(labelColumn)
  newRow.appendChild(inputColumn)
  newRow.appendChild(exampleColumn)
  estimationTable.appendChild(newRow)
  rangeInput.dispatchEvent(new Event('input'))
}

const initEstimationTopics = (topics) => {
  const estimationTable = document.querySelector('.estimationTopicsContainer')
  /**
   *
   * @param {InputEvent & { target: HTMLInputElement }} e
   */
  const onInputCallback = (e) => {
    update_total()
    e.target.parentNode.previousSibling.querySelector('.rowEstimate').innerText = e.target.value
    e.target.parentNode.nextSibling.innerText = getEstimationTopicExample(topics, e.target.getAttribute('name'), e.target.value)
  }

  topics.forEach(topic => {renderEstimationTopic(topic, estimationTable, onInputCallback)})
}

const startVotingPage = (estimationSet) => {

  loadEstimationTopics(estimationSet).then( (topics) => {
    initEstimationTopics(topics)
    init_sse()
  })
}

const getEstimationTopicExample = (estimationTopics, topicName, estimation) => {
  console.log(topicName, estimation)
  return estimationTopics.find(t => t.name === topicName).examples[estimation]
}

const copyResultsURL = (code, ticketId) => {

  var url = document.location.origin + get_ticket_results_path(code, ticketId)
  navigator.clipboard.writeText(url)

  document.getElementById('getResultsURLFeedback').innerText = "Link copied to the clipboard"
}
