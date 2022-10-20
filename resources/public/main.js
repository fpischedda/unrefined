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

function copy_estimation_link (suffix='') {
  var url = document.location.href + suffix
  navigator.clipboard.writeText(url)
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

function goto_estimation_page (code, ticket_id) {
  document.location.href = '/refine/' + code + '/ticket/' + ticket_id + '/estimate'
}

function goto_watch_page (code, ticket_id) {
  document.location.href = '/refine/' + code + '/ticket/' + ticket_id
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
  const url = '/refine/' + code + '/ticket/' + ticket_id + '/events'

  console.log('connecting to SSE endpoint ' + url)
  connect_to_events(url, handle_sse_messages)
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

function start_voting_page () {

  const estimationTable = document.querySelector('.estimationTopicsContainer')
  /**
   * 
   * @param {InputEvent & { target: HTMLInputElement }} e 
   */
  const onInputCallback = (e) => {
    update_total()
    e.target.parentNode.previousSibling.querySelector('.rowEstimate').innerText = e.target.value
    e.target.parentNode.nextSibling.innerText = getEstimationTopicExample(e.target.getAttribute('name'), e.target.value)
  }
  estimationTopics.forEach(topic => {
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
    rangeInput.max = topic.maxValue
    rangeInput.name = topic.name
    rangeInput.oninput = onInputCallback
    inputColumn.appendChild(rangeInput)
    newRow.appendChild(labelColumn)
    newRow.appendChild(inputColumn)
    newRow.appendChild(exampleColumn)
    estimationTable.appendChild(newRow)
    rangeInput.dispatchEvent(new Event('input'))
  })

  init_sse()
}

function load_ticket_preview (refinement_code, ticket_id) {

  const url = '/refine/' + refinement_code + '/ticket/' + ticket_id + '/preview'
  console.log('feching preview from ', url)

  fetch(url)
    .then(response => { return response.text()})
    .then(preview => {
      console.log('preview:')
      console.log(preview)
      document.getElementById('ticket-preview').innerHTML = preview
    })
}

const estimationTopics = [
  {
    name: 'implementation',
    label: 'Code implementations (endpoints, Airflow, models, queries, etc)',
    maxValue: 8,
    examples: [
      'no change',
      'Add authorization check (one if condition added)',
      'Implement GetEntityMetadata (just return statement) and showing it in FE',
      'Parse a simple CSV file in airline UpdateAirlineData endpoint and expose it to cargo.drive',
      '',
      'Add an attribute to shipment model and update 3 airlines and 3 FEs to work with it',
      '',
      '',
      '6 endpoints deep changes (i.e. link sharing)',
    ],
  },
  {
    name: 'backend',
    label: 'Backend API changes (protos)',
    maxValue: 3,
    examples: [
      'no change',
      'Small amount of API changes (e.g. 2 new attributes in a message)',
      'Considerable amount of API changes (e.g. 5 new messages, including nested ones)',
      'Huge amount of API changes (e.g. 2 new airlines at the same time + 2 new airline-specific additional data messages)',
    ],
  },
  {
    name: 'migrations',
    label: 'Migrations',
    maxValue: 5,
    examples: [
      'no change',
      'Any schema migration changes in 1 db',
      '',
      '',
      '',
      'Change the schema in all services',
    ],
  },
  {
    name: 'data_migrations',
    label: 'Data migrations',
    maxValue: 5,
    examples: [
      'no change',
      'Populate all NULL rows with a default value',
      '',
      '',
      '',
      'Migration from stations to organizations from HubSpot (third-party system)',
    ],
  },
  {
    name: 'testing',
    label: 'Automated testing',
    maxValue: 3,
    examples: [
      'no change',
      'Creating or updating few test scenarios (e.g. 2 new integration tests + 1 new unit test)',
      'Creating or updating several test scenarios (e.g. 3 new integration tests + slight change to e2e test + update 2 unit tests)',
      'Creating or updating a lot of test scenarios (e.g. 5 new integration tests + 2 updated integration tests + 4 new unit tests)',
    ],
  },
  {
    name: 'manual_testing',
    label: 'External dependency on manual testing',
    maxValue: 1,
    examples: [
      'no change',
      'Feature branch on staging for someone to test (if we think there will be challenges with running the test and/or follow-ups and coordination with an engineer are required)',
    ],
  },
  {
    name: 'risk',
    label: 'Risk',
    maxValue: 2,
    examples: [
      'no change',
      'Slight risk',
      'A lot of risk, we are very uncertain',
    ],
  },
  {
    name: 'complexity',
    label: 'Complexity',
    maxValue: 3,
    examples: [
      'no change',
      'There might be hidden complexities',
      '',
      'We need a design document',
    ],
  },
]

const getEstimationTopicExample = (topicName, estimation) => {
  console.log(topicName, estimation)
  return estimationTopics.find(t => t.name === topicName).examples[estimation]
}
