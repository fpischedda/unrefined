function get_refinement_code() {
  const code = document.getElementsByTagName('body')[0].dataset['refinement'];
  console.log('session code: ' + code);
  return code;
}

// when the google chart library will be loaded this will hold a reference
// to the chart object, used for rendering
var g_chart = null;

function update_vote_stats(payload) {
  document.getElementById('total-voted').textContent = payload.voted;
  document.getElementById('total-skipped').textContent = payload.skipped;

  var raw_data = [['Estimation', 'Number of votes'],];

  payload.votes.forEach(i => {
    raw_data.push(['Vote: ' + i.vote, i.count]);
  });

  const data = google.visualization.arrayToDataTable(raw_data);

  g_chart.draw(data, {title: 'Distribution of votes'});
}

function handle_sse_messages(e) {
  const data = JSON.parse(e.data);
  console.log(data);

  if( data.event == 'user-voted' || data.event == 'user-skipped' ) {
    update_vote_stats(data.payload);
  }
}

function start() {

  const code = get_refinement_code();
  const url = '/refine/' + code + '/events';

  google.charts.load('current', {'packages':['corechart']});
  google.charts.setOnLoadCallback( e => {
    const elem = document.getElementById('vote-chart');
    g_chart = new google.visualization.PieChart(elem);
  });



  console.log('connecting to SSE endpoint ' + url)
  connect_to_events(url, handle_sse_messages);
}

start();

async function post_data(url, data) {

  const post = {method: 'POST',
	      credentials: 'same-origin',
	      headers: {'Content-Type': 'application/json'},
	      body: JSON.stringify(data)}

  const response = await fetch(url, post);

  return response.json();
}

function create_ticket() {
  var code = get_refinement_code();
  var ticket_id = document.getElementById('new-ticket-id');
  var data = {ticket_id: ticket_id.value,
	      code: code}

  var url = '/api/refinement/' + code + '/ticket';

  post_data(url, data).then( data => {
    console.log('create ticket response');
    console.log(data);
    ticket_id.value(null);
  })

  return false;
}

function send_vote() {
  var code = get_refinement_code();
  var ticket_id = document.getElementById('current-ticket');
  var vote = document.getElementById('vote');
  var data = {vote: vote.value}

  var url = '/api/refinement/' + code + '/ticket/vote';

  post_data(url, data).then( data => {
    console.log('vote response');
    console.log(data);
    ticket_id.value(null);
  })

  return false;
}

function copy_estimation_link(suffix='') {
  var url = document.location.href + suffix;
  navigator.clipboard.writeText(url);
}
