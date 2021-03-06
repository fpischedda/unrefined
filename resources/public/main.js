function get_refinement_code() {
  const code = document.getElementsByTagName('body')[0].dataset['refinement'];
  console.log('session code: ' + code);
  return code;
}

function get_ticket_id() {
  const ticket = document.getElementsByTagName('body')[0].dataset['ticket'];
  console.log('ticket id: ' + ticket);
  return ticket;
}

function copy_estimation_link(suffix='') {
  var url = document.location.href + suffix;
  navigator.clipboard.writeText(url);
}

// when the google chart library will be loaded this will hold a reference
// to the chart object, used for rendering
var g_chart = null;

function update_vote_stats(payload) {

  if( g_chart == null) { return; }

  document.getElementById('total-voted').textContent = payload.voted;
  document.getElementById('total-skipped').textContent = payload.skipped;

  var raw_data = [['Estimation', 'Number of votes'],];

  payload.votes.forEach(i => {
    raw_data.push(['Points: ' + i.points, i.count]);
  });

  const data = google.visualization.arrayToDataTable(raw_data);

  g_chart.draw(data, {title: 'Distribution of votes'});
}

function goto_estimation_page(code, ticket_id) {
  document.location.href = '/refine/' + code + '/ticket/' + ticket_id + '/estimate';
}

function handle_sse_messages(e) {
  const data = JSON.parse(e.data);
  console.log(data);

  if( data.event == 'user-voted' || data.event == 'user-skipped' || data.event == 'ticket-status') {
    update_vote_stats(data.payload);
  }
  else if( data.event == 're-estimate-ticket' && document.location.href.includes('estimate')) {
    goto_estimation_page(data.payload.code, data.payload.ticket_id);
  }
  else if( data.event == 'added-ticket') {
    if(data.payload.ticket_id != get_ticket_id()) {
      goto_estimation_page(data.payload.code, data.payload.ticket_id);
    }
  }
}

function init_sse() {
  const code = get_refinement_code();
  const ticket_id = get_ticket_id();
  const url = '/refine/' + code + '/ticket/' + ticket_id + '/events';

  console.log('connecting to SSE endpoint ' + url)
  connect_to_events(url, handle_sse_messages);
}

function start() {

  google.charts.load('current', {'packages':['corechart']});
  google.charts.setOnLoadCallback( e => {
    const elem = document.getElementById('vote-chart');
    g_chart = new google.visualization.PieChart(elem);

    init_sse();
  });
}
