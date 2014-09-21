<%@ page language="java" contentType="text/html; charset=US-ASCII"
	pageEncoding="US-ASCII"%>
<%@ page import="java.util.*"%>
<%@ page import="java.text.*"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>FSB - FAX Status Board</title>

<link rel="stylesheet" href="static/ilija.css" type="text/css">
<link rel="stylesheet" type="text/css"
	href="//code.jquery.com/ui/1.10.3/themes/redmond/jquery-ui.css">

<link rel="stylesheet" type="text/css"
	href="//cdn.datatables.net/plug-ins/725b2a2115b/integration/jqueryui/dataTables.jqueryui.css">
	
<link href="vis/dist/vis.css" rel="stylesheet" type="text/css" />

<script type="text/javascript" language="javascript"
	src="//code.jquery.com/jquery-1.11.1.min.js"></script>
<script src="http://code.jquery.com/ui/1.10.3/jquery-ui.js"></script>


<script type="text/javascript" language="javascript"
	src="//cdn.datatables.net/1.10.2/js/jquery.dataTables.min.js"></script>

<script src="http://code.highcharts.com/highcharts.js"></script>
<script src="http://code.highcharts.com/modules/heatmap.js"></script>
<script src="http://code.highcharts.com/modules/exporting.js"></script>

<script
	src="https://maps.googleapis.com/maps/api/js?v=3.exp&sensor=false"></script>
  
<script src="vis/dist/vis.js"></script>

<script src="static/dynamic.js"></script>
</head>
<body>


	<%
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.add(Calendar.HOUR_OF_DAY, +1);
		Date date = cal.getTime();
		String dateFormat = "yyyy-MM-dd";
		String hourFormat = "HH00";
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		SimpleDateFormat shf = new SimpleDateFormat(hourFormat);
		sdf.setTimeZone(cal.getTimeZone());
		shf.setTimeZone(cal.getTimeZone());
		String fDate = sdf.format(date) + "T" + shf.format(date);
	%>

	<script>
    fDate="<%=fDate%>";
	</script>

	<script>
	

	
		function ip2sip(ip){
	 		o1 = Math.floor(ip / 16777216) % 256;
     		o2 = Math.floor(ip / 65536) % 256
     		o3 = Math.floor(ip / 256) % 256
     		o4 = Math.floor(ip) % 256
     		return o1+'.'+o2+'.'+o3+'.'+o4
		}
		
		var traceMarkers=[];
		var traceNodes;
		var tracemap;
		var tracegeodesic;
		var tracegeodesics=[];
		var tracegeodesicOptions = { strokeOpacity: 1.0, geodesic: true };
		var traceinfowindow;
		function drawHops(originIP){
			console.log("draw hOPs");
			//clear paths
			for (var i=0;i<tracegeodesics.length;i++) tracegeodesics[i].setMap(null);
			//console.log("origin:"+originIP);
			for (var i=0;i<traceNodes.length;i++){
				if (traceNodes[i].ip!=originIP) continue;
				tracemap.panTo(new google.maps.LatLng(traceNodes[i].lat, traceNodes[i].long));
				//console.log("ups:"+traceNodes[i].up.length+"   downs:"+traceNodes[i].down.length);
 				for (j=0;j<traceNodes.length;j++){
 					for (k=0;k<traceNodes[i].up.length;k++){
						if (traceNodes[j].ip==traceNodes[i].up[k][0]){
							//console.log("u> "+traceNodes[j].ip);
							tracegeodesic = new google.maps.Polyline(tracegeodesicOptions);
							tracegeodesic.setOptions({strokeColor: '#FF0000',strokeWeight: 3.0}); 
							tracegeodesic.setMap(tracemap);
							var gPath = tracegeodesic.getPath();
							gPath.push(traceMarkers[i].getPosition());
							gPath.push(traceMarkers[j].getPosition());
							tracegeodesics.push(tracegeodesic);
						}
 					}
 					for (k=0;k<traceNodes[i].down.length;k++){
						if (traceNodes[j].ip==traceNodes[i].down[k][0]){
							//console.log("<d "+traceNodes[j].ip);
							tracegeodesic = new google.maps.Polyline(tracegeodesicOptions);
							tracegeodesic.setOptions({strokeColor: '#0000FF',strokeWeight: 3.0}); 
							tracegeodesic.setMap(tracemap);
							var gPath = tracegeodesic.getPath();
							gPath.push(traceMarkers[i].getPosition());
							gPath.push(traceMarkers[j].getPosition());
							tracegeodesics.push(tracegeodesic);
						}
					}
				}
			}


/* 							google.maps.event.addListener(tracegeodesic, 'click', function(event) {
								cont="";
								if ($("#idMapTimeScale option:selected").val()=="destination")
									cont="source:<b>"+sitename+"</b><br>destination: <b>"+marker.title+"</b><br>rate: <b>"+rates[s]+"</b>";
								else
									cont="destination:<b>"+sitename+"</b><br>source: <b>"+marker.title+"</b><br>rate: <b>"+rates[s]+"</b>";
								infowindow = new google.maps.InfoWindow({content: cont});
								infowindow.open(map,event.latLng);
							}); */


		}
		
		function showGraph(){
			var nodes = [];
			var edges = [];
			nodes.push({id:9999, label: $("#idDestination option:selected").text(), shape: 'circle'});
			$.getJSON( "mongotraces", {source:$("#idSource option:selected").text(),destination:$("#idDestination option:selected").text()}, function(netw) {
				if (netw==null) {
					console.log("No data.");
					return;
				}
				for (var i=0; i<netw.nodes.length;i++){
					if (netw.nodes[i].name!="") lab=netw.nodes[i].name; else lab=netw.nodes[i].sip;
					nodes.push({id: netw.nodes[i].ip, label: lab})
				}
				
				edges=netw.edges;
				edges.splice(0,0,{'from':9999,'to':netw.edges[0]['from']});
				var container = document.getElementById('mynetwork');
				var data = { nodes: nodes, edges: edges };
				var options = {};
				var network = new vis.Network(container, data, options);
			});
		}
		
		function showTopology(){
/* 			var nodes = [];
			var edges = []; */
			//nodes.push({id:9999, label: $("#idDestination option:selected").text(), shape: 'circle'});
			$.getJSON( "topology", {}, function(netw) {
				if (netw==null) {
					console.log("No data.");
					return;
				}
		/* 		for (var i=0; i<netw.nodes.length;i++){
					if (netw.nodes[i].name!="") lab=netw.nodes[i].name; else lab=netw.nodes[i].sip;
					nodes.push({id: netw.nodes[i].ip, label: lab})
				} */
				
				/* edges=netw.edges; */
				/* edges.splice(0,0,{'from':9999,'to':netw.edges[0]['from']}); */
				var container = document.getElementById('topology');
				var data = { nodes: netw.nodes, edges: netw.edges };
				var options = {};
				var network = new vis.Network(container, data, options);
			});
		}
		
		function showTraceroutes() {
			      				
			$("#loading").show();
			var mapOptions = {
				zoom : 2,
				center : new google.maps.LatLng(20.0, 0.0)
			};
			tracemap = new google.maps.Map(document
					.getElementById('map-traceroutes'), mapOptions);

			$.getJSON( "mongotraces", {}, function(nodes) {
					//console.log(nodes); 
					traceNodes=nodes;
					for (var i=0; i<nodes.length;i++){
						var marker = new google.maps.Marker({
							position : new google.maps.LatLng(  nodes[i].lat,nodes[i].long), map : tracemap, title : nodes[i].name
						});
						setMarkerIP(marker, nodes[i]);
						traceMarkers.push(marker);
					}	
				});

			$("#loading").hide();
		}

 		function setMarkerIP(marker, node) {
 			var con='<b>'+node.name+"</b><br>"+ip2sip(node.ip)+'<br>Upstream:<br>';
 			for (i=0;i<node.up.length;i++)con+=ip2sip(node.up[i][0])+' '+node.up[i][1]+'<br>';
 			con+='Downstream:<br>';
 			for (i=0;i<node.down.length;i++)con+=ip2sip(node.down[i][0])+' '+node.down[i][1]+'<br>';
 			var infowindow = new google.maps.InfoWindow({ content: con, size: new google.maps.Size(50,50)});
			google.maps.event.addListener(marker, 'click', function() {
				infowindow.open(tracemap,marker);
				drawHops(node.ip);
				});
		} 
		
		function showCostMatrix() {
			$("#loading").show();
			hcoptions = {
				chart : {
					type : 'heatmap',
					marginTop : 40,
					marginBottom : 40
				},
				title : {
					text : 'xrdcp rates from FAX endpoints to WNs'
				},
				xAxis : {
					categories : [], title : 'destinations'
				},
				yAxis : {
					categories : [], title : 'sources'
				},
				colorAxis : {
					stops : [ [ 0, '#ff0000' ], [ 0.05, '#00ff00' ], [ 1.0, '#0000FF' ] ], min : 0, max:55	
				}, 
				legend : {
					align : 'right', layout : 'vertical', margin : 0, verticalAlign : 'top', y : 25, symbolHeight : 320
				},
				tooltip : {
					formatter : function() {
						return '<b>'
								+ this.series.xAxis.categories[this.point.x]
								+ '</b> copied at in average <br><b>' + this.point.value
								+ '</b> MB/s from <br><b>'
								+ this.series.yAxis.categories[this.point.y]
								+ '</b>';
					}
				},
				series : [ {
					name : 'xrdcp rates',
					borderWidth : 1,
					data : [],
					dataLabels : {
						enabled : false,
						color : 'black',
						style : {
							textShadow : 'none'
						}
					}
				} ],
				exporting : { buttons : { contextButton : { text : 'Export' } }, sourceHeight : 1050, sourceWidth : 1485 },
				credits : {
					enabled : false
				}
			}

			$.getJSON("wancost", {
				costmatrix : $("#idCostMatrixTimeScale option:selected").val()
			}, function(data) {
				hcoptions.xAxis.categories = data.destinations;
				hcoptions.yAxis.categories = data.sources;
				hcoptions.series[0].data = [];
				so = data.sources.length;
				de = data.destinations.length;
				console.log("sour: " + so);
				console.log("dest: " + de);
				for (var d = 0; d < de; d++) {
					for (var s = 0; s < so; s++) {
						z = data.links[d * so + s];
						if (z > 50) z = 50.0;
						if (z >= 0 && z<=50.0 )
							hcoptions.series[0].data.push([ d, s, z ]);
					}
				}
				$('#costmatrixspace').highcharts(hcoptions);
			});
			$("#loading").hide();
		}

		function showFailover() {

			$.ajaxSetup({
				async : false
			});
			var options = {
				chart : { renderTo : 'failovergraphspace', zoomType : 'xy', type : 'column', height : 600, margin : [ 60, 30, 45, 70 ] },
				plotOptions : { column : { stacking : 'normal', pointRange : 4 * 3600 * 1000, pointPadding : 0, groupPadding : 0 } },
				title : { text : '' },
				xAxis : { type : 'datetime', tickWidth : 0, gridLineWidth : 1, title : { text : 'Date' } },
				yAxis : { title : { text : '' } },
				legend : { align : 'left', verticalAlign : 'top', y : 10, floating : true, borderWidth : 0 },
				exporting : { buttons : { contextButton : { text : 'Export' } }, sourceHeight : 1050, sourceWidth : 1485 },
				credits : { enabled : false }
			// tooltip: { formatter: function() { return '<b>'+ this.series.name +'</b><br/>'+ Highcharts.dateFormat('%e. %b', this.x) +': '+ this.y +' m';} },
			};


			$.getJSON("UpdateFailover",	{
				failovertime : $("#idFailoverTimeScale option:selected").val(),
				failoverplot : $("#idFailoverPlotSelect option:selected").val(),
				failoversite : $("#idFailoverSiteSelect option:selected") .val()
				}, function(data) {
					options.series = data.plot;
					options.title.text="Failover "+$("#idFailoverPlotSelect option:selected").val()+" at "+ $("#idFailoverSiteSelect option:selected") .val() +" queue"
					chart = new Highcharts.Chart(options);
					$('#failovertablespace').dataTable({
								"bJQueryUI" : true,
								"data" : data.tableData.data,
								"columns" : data.tableData.headers,
								"bDestroy" : true
								});

							});

		}

		function showOverflow() {

			$.ajaxSetup({
				async : false
			});
			var options = {
				chart : {
					renderTo : 'overflowgraphspace',
					zoomType : 'xy',
					type : 'column',
					height : 600,
					margin : [ 60, 30, 45, 70 ]
				},
				plotOptions : {
					column : {
						stacking : 'normal',
						pointRange : 3600 * 1000,
						pointPadding : 0,
						groupPadding : 0
					}
				},
				title : {
					text : ''
				},
				xAxis : {
					type : 'datetime',
					tickWidth : 0,
					gridLineWidth : 1,
					title : {
						text : 'Date'
					}
				},
				yAxis : {
					title : {
						text : ''
					}
				},
				legend : {
					align : 'left',
					verticalAlign : 'top',
					y : 10,
					floating : true,
					borderWidth : 0
				},
				exporting : {
					buttons : {
						contextButton : {
							text : 'Export'
						}
					},
					sourceHeight : 1050,
					sourceWidth : 1485
				},
				credits : {
					enabled : false
				},
				tooltip : {
					formatter : function() {
						return '<b>' + this.series.name + '</b><br/>'
								+ Highcharts.dateFormat('%e. %b', this.x)
								+ ': ' + this.y;
					}
				},
			};

			$.getJSON( "CopyOverflow", {
								overflowtime : $(
										"#idOverflowTimeScale option:selected")
										.val(),
								overflowplot : $(
										"#idOverflowPlotSelect option:selected")
										.val(),
								overflowdest : $(
										"#idOverflowDestinationSelect option:selected")
										.val()
							}, function(data) {
								options.series = data.plot;
								chart = new Highcharts.Chart(options);
								$('#overflowtablespace').dataTable({
									"bJQueryUI" : true,
									"data" : data.tableData.data,
									"columns" : data.tableData.headers,
									"bDestroy" : true
								});

							});
			
		}
	</script>
	<div class="maincolumn">

		<div class="mainheading">
			<a href="http://atlas.web.cern.ch/Atlas/Collaboration/"> <img
				border="0" src="static/atlas_logo.jpg" alt="ATLAS main page">
			</a>
			<div id="maintitle">FAX Status Board</div>
		</div>


		<div id="tabs">
			<ul>
				<li><a href="#tabs-1">Endpoints Status</a></li>
				<li><a href="#tabs-2">Redirectors Status</a></li>
				<li><a href="#tabs-3">Map</a></li>
				<li><a href="#tabs-4">Link rates</a></li>
				<li><a href="#tabs-5">Cost Matrix</a></li>
				<li><a href="#tabs-6">Stability</a></li>
				<li><a href="#tabs-7">Failover</a></li>
				<li><a href="#tabs-8">Overflow</a></li>
				<li><a href="#tabs-9">Traceroutes</a></li>
				<li><a href="#tabs-10">Docs and links</a></li>
				<li><a href="#tabs-11">Topology</a></li>
			</ul>
			<div id="tabs-1">
				<br>
				<table cellpadding="0" cellspacing="0" border="0" class="display"
					id="epStatus" width="100%">
					<thead>
						<tr>
							<th></th>
							<th>Endpoint</th>
							<th>Direct</th>
							<th>Upstream redirection</th>
							<th>Downstream redirection</th>
							<th>ATLAS readonly</th>
						</tr>
					</thead>
					<tbody></tbody>
				</table>
			</div>

			<div id="tabs-2">
				<br>
				<table cellpadding="0" cellspacing="0" border="0" class="display"
					id="reStatus" width="100%">
					<thead>
						<tr>
							<th></th>
							<th>Redirector</th>
							<th>Upstream redirection</th>
							<th>Downstream redirection</th>
							<th></th>
						</tr>
					</thead>
					<tbody></tbody>
				</table>
			</div>

			<div id="tabs-3">
				<select id="idMapTimeScale">
					<option value="destination">show rates to site</option>
					<option value="source">show rates from site</option>
				</select>
				<div id="map-canvas" style="height: 640px"></div>
			</div>
			<div id="tabs-4">
				Source <select id="idSource"></select> Destination <select
					id="idDestination"></select> Time bin:<select id="idTimeZoom">
					<option value="FAXcost">no Binning</option>
					<option value="FAXcost30min" selected>30 min</option>
					<option value="FAXcost1h">1h</option>
					<option value="FAXcost3h">3h</option>
					<option value="FAXcost6h">6h</option>
					<option value="FAXcost12h">12h</option>
					<option value="FAXcost24h">24h</option>
				</select> <br>
				<div id="graphspace"></div>
				<div id="mynetwork" style="width: 100%; height: 900px; border: 1px solid lightgray;"></div>
			</div>
			<div id="tabs-5">
				Time scale:<select id="idCostMatrixTimeScale">
					<option value=6 selected>6h</option>
					<option value=12>12h</option>
					<option value=24>24h</option>
					<option value=48>48h</option>
					<option value=168>last week</option>
					<option value=336>last 2 weeks</option>
					<option value=672>last month</option>
					<option value=8760>last year</option>
					<option value=99999>all the time</option>
				</select>
				<div id="costmatrixspace" style="height: 1050px"></div>
			</div>
			<div id="tabs-6">
				Test type:<select id="idStability">
					<option value=direct selected>Direct</option>
					<option value=upstream>Upstream</option>
					<option value=downstream>Downstream</option>
					<option value=x509>x509</option>
				</select> Time scale:<select id="idStabilityTimeScale">
					<option value=24 selected>24 h</option>
					<option value=48>48 h</option>
					<option value=168>week</option>
					<option value=336>two weeks</option>
					<option value=672>month</option>
				</select>
				<div id="stabilityspace" style="height: 1100px"></div>
			</div>
			<div id="tabs-7">
				Time scale:<select id="idFailoverTimeScale">
					<option value=6>6h</option>
					<option value=12>12h</option>
					<option value=24 selected>24h</option>
					<option value=48>48h</option>
					<option value=168>last week</option>
					<option value=336>last 2 weeks</option>
					<option value=672>last month</option>
					<option value=8760>last year</option>
				</select> Queue:<select id="idFailoverSiteSelect"></select> Show:<select
					id="idFailoverPlotSelect">
					<option selected>jobs</option>
					<option>Files</option>
					<option>Data size</option>
					<option>Transfer duration</option>
				</select>
				<div id="failovergraphspace"></div>
				<br> <br>
				<table cellpadding="0" cellspacing="0" border="0" class="display"
					id="failovertablespace" width="100%">
					<thead></thead>
					<tbody></tbody>
				</table>
			</div>
			<div id="tabs-8">
				Time scale:<select id="idOverflowTimeScale">
					<option value=6>6h</option>
					<option value=12>12h</option>
					<option value=24 selected>24h</option>
					<option value=48>48h</option>
					<option value=168>last week</option>
					<option value=336>last 2 weeks</option>
					<option value=672>last month</option>
					<option value=8760>last year</option>
				</select> Destination:<select id="idOverflowDestinationSelect"></select>
				Show:<select id="idOverflowPlotSelect">
					<option selected>jobs</option>
					<option>Avg. wait time</option>
					<option>Avg. duration</option>
					<option>Avg. CPU time</option>
				</select>
				<div id="overflowgraphspace"></div>
				<br>
				<table cellpadding="0" cellspacing="0" border="0" class="display"
					id="overflowtablespace" width="100%">
					<thead></thead>
					<tbody></tbody>
				</table>
			</div>
			<div id="tabs-9">
				<select id="idMapTraceroutes">
					<option value="hosts">show hosts</option>
					<option value="hops">show hops</option>
					<option value="paths">show paths</option>
				</select>
				<div id="map-traceroutes" style="height: 640px"></div>
			</div>
			<div id="tabs-10">

				<b> <a
					href="https://twiki.cern.ch/twiki/bin/view/AtlasComputing/AtlasXrootdSystems">Homepage</a>
					<br> <a href="https://its.cern.ch/jira/browse/FAX">Jira</a> <br>
					<a href="http://git.cern.ch/pubweb/FAX.git">Git repository</a> <br>
				</b>

				<H2>TWiki</H2>
				<a
					href="https://twiki.cern.ch/twiki/bin/view/AtlasComputing/FaxCoverage">Current
					coverage</a><br> <a
					href="https://twiki.cern.ch/twiki/bin/view/AtlasComputing/RemotelyObtained">Remotely
					Obtained XRootD information</a>
				<H2>Site Status Board (SSB)</H2>
				<a
					href="http://dashb-atlas-ssb.cern.ch/dashboard/request.py/siteview#currentView=FAX+endpoints&fullscreen=true&highlight=false">Endpoints
					tests</a> *Direct, upstream, downstream, X509*<br> <a
					href="http://dashb-atlas-ssb.cern.ch/dashboard/request.py/siteview#currentView=FAX+redirectors&fullscreen=true&highlight=false">Redirectors
					test</a> *Upstream, downstream*<br> <a
					href="http://dashb-atlas-ssb.cern.ch/dashboard/request.py/siteview#currentView=FAX+cost+matrix&fullscreen=true&highlight=false">FAX
					cost matrix</a> *Transfer rates by xrdcp*

				<H2>XRootD based monitoring</H2>
				<a href="http://atl-prod07.slac.stanford.edu:8080/">MonaLisa FAX
					dashboard</a> <br> <a
					href="http://dashb-atlas-xrootd-transfers.cern.ch/ui">Transfers
					dashboard</a> <br> <a
					href="http://dashb-ai-621.cern.ch/cosmic/DB_ML_Comparator/">ML
					DDM consistency checks</a>

			</div>
		<div id="tabs-11">
				<div id="topology" style="width: 100%; height: 900px; border: 1px solid lightgray;"></div>
		</div>
		</div>

	</div>

	<div id="loading" style="display: none">
		<br> <br>Loading data. Please wait...<br> <br> <img
			src="static/wait_animated.gif" alt="loading" />
	</div>


</body>
</html>