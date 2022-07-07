function get_refinement_code() {
  var code = document.getElementById('refinement-code').value;
  console.log('session code: ' + code);
  return code;
}

function handle_sse_messages(e) {
  const data = JSON.parse(e.data);
  console.log(data);

  if( data.event == 'user-voted' || data.event == 'user-skipped' ) {
    document.getElementById('total-voted').textContent = data.payload.voted;
    document.getElementById('total-skipped').textContent = data.payload.skipped;
  }
}

function start() {

  var code = get_refinement_code();
  var source = connect_to_events('/refine/' + code + '/events',
				 handle_sse_messages);
}

start();

async function post_data(url, data) {

  var post = {method: 'POST',
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

function copy_estimation_link() {
  var url = document.location.href + '/estimate';
  navigator.clipboard.writeText(url);
}

function revel_results() {
  var url = document.location.href + '/reveal';
  document.location.href = url;
}
