<%@ page language="java" contentType="text/html; charset=US-ASCII" pageEncoding="US-ASCII"%>
<%@ page import="java.util.*" %>
<%@ page import="java.text.*" %>
	
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>FSB - FAX Status Board</title>

<link rel="stylesheet" href="static/ilija.css" type="text/css">
<link rel="stylesheet" type="text/css" href="//code.jquery.com/ui/1.10.3/themes/redmond/jquery-ui.css">

<link rel="stylesheet" type="text/css" href=
	"//cdn.datatables.net/plug-ins/725b2a2115b/integration/jqueryui/dataTables.jqueryui.css">
	

<script type="text/javascript" language="javascript" src="//code.jquery.com/jquery-1.11.1.min.js"></script>
<script src="http://code.jquery.com/ui/1.10.3/jquery-ui.js"></script>


<script type="text/javascript" language="javascript" src="//cdn.datatables.net/1.10.2/js/jquery.dataTables.min.js"></script>
	
<script src="http://code.highcharts.com/highcharts.js"></script>
<script src="http://code.highcharts.com/modules/heatmap.js"></script>
<script src="http://code.highcharts.com/modules/exporting.js"></script>

<script
	src="https://maps.googleapis.com/maps/api/js?v=3.exp&sensor=false"></script>

	<script src="static/dynamic.js"></script>
</head>
<body>

    
    <% 
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	cal.add(Calendar.HOUR_OF_DAY, +1 );
	Date date = cal.getTime();
	String dateFormat = "yyyy-MM-dd";
	String hourFormat = "HH00";
	SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
	SimpleDateFormat shf = new SimpleDateFormat(hourFormat);
	sdf.setTimeZone(cal.getTimeZone());
	shf.setTimeZone(cal.getTimeZone());
    String fDate=sdf.format(date) + "T" + shf.format(date);
    %>
    
    <script>
    fDate="<%= fDate %>";
    </script>
    
    <script>
	
    function showCostMatrix(){
    	$("#loading").show();
    	hcoptions={
    			chart: { type: 'heatmap', marginTop: 40, marginBottom: 40 },
    			title: { text: 'xrdcp rates from FAX endpoints to WNs'},
    			xAxis: { categories:[], title: 'destinations' },
    			yAxis: { categories:[], title: 'sources' },
    			colorAxis: { stops: [ [0, '#ff0000'], [0.05, '#00ff00'], [1, '#0000FF'] ], min: 0 },
    			legend: { align: 'right', layout: 'vertical', margin: 0, verticalAlign: 'top', y: 25, symbolHeight: 320 },
    			tooltip: {
    				formatter: function () {
    					return '<b>' + this.series.xAxis.categories[this.point.x] + '</b> copied <br><b>' +
    					this.point.value + '</b> in average from <br><b>' + this.series.yAxis.categories[this.point.y] + '</b>';
    				}
    			},
    			series: [{
    				name: 'xrdcp rates', borderWidth: 1, data: [],
    				dataLabels: { enabled: false, color: 'black', style: { textShadow: 'none' } }
    			}],
    			exporting: {     buttons: { contextButton: {  text: 'Export' }  }, sourceHeight:1050, sourceWidth: 1485 },
    			credits: { enabled: false }
    	}

    	$.getJSON("wancost",{costmatrix:$("#idCostMatrixTimeScale option:selected").val()}, function(data){
    			hcoptions.xAxis.categories=data.destinations;
    			hcoptions.yAxis.categories=data.sources;
    			hcoptions.series[0].data=[];
//     	    	console.log(data.destinations);
//    	    	console.log(data.sources);
    	    	so=data.sources.length;
    	    	de=data.destinations.length;
    			for(var i = 0; i < so; i++) {
    				for(var j = 0; j < de; j++) {
    					var z=data.links[i*de+j];
    					if (z>=0) hcoptions.series[0].data.push( [ j, i, z ] );
    				}
    			}
    			$('#costmatrixspace').highcharts(hcoptions);
    	} );
    	$("#loading").hide();
    }
    
    function showFailover(){

    	$.ajaxSetup({async: false});
    	var options = {
    			chart : {
    				renderTo : 'failovergraphspace', zoomType : 'xy',
    				type : 'column', height : 600, margin : [ 60, 30, 45, 70 ]
    			},
    			plotOptions: {  series: { pointRange: 4 * 3600 * 1000 } },
    			title : { text : '' },
    			xAxis : { type : 'datetime', tickWidth : 0, gridLineWidth : 1, title : { text : 'Date' } },
    			yAxis : { title : { text : '' } },
    			legend : { align : 'left', verticalAlign : 'top', y : 10, floating : true, borderWidth : 0 },
    			exporting: {     buttons: { contextButton: {  text: 'Export' } }, sourceHeight:1050, sourceWidth: 1485    },
    			credits: { enabled: false }
    			// tooltip: { formatter: function() { return '<b>'+ this.series.name +'</b><br/>'+ Highcharts.dateFormat('%e. %b', this.x) +': '+ this.y +' m';} },
    	}; 
    	    	
/*     	$.get("wanio", {failover:$("#idFailoverTimeScale option:selected").val()}, function(msg) {
    		if (msg.length) {
    			msg = msg.trim().split(/\n/g);
    			jQuery.each(msg, function(i, line) {
    				line = line.split(/\t/);
    				failoverTable.fnAddData([ '<img src="static/details_open.png">', line[0], line[1], line[2], line[3], line[4], line[5], line[6], line[7], line[8] ]);
    			});
    		}
    	}); */
    	
    	$.getJSON("UpdateFailover", {failovertime:$("#idFailoverTimeScale option:selected").val(), failoverplot:$("#idFailoverPlotSelect option:selected").val(), failoversite:$("#idFailoverSiteSelect option:selected").val()}, function(data) {
    		options.series=data.plot;
    		chart = new Highcharts.Chart(options); 
    		$('#failovertablespace').dataTable( {
		    	"bJQueryUI": true,
		        "data": data.tableData.data,
		        "columns": data.tableData.headers,
		        "bDestroy": true
		    } );    
		    
    	});

    }
    
    function showOverflow(){

    	$.ajaxSetup({async: false});
    	var options = {
    			chart : {
    				renderTo : 'overflowgraphspace', zoomType : 'xy',
    				type : 'column', height : 600, margin : [ 60, 30, 45, 70 ]
    			},
    			plotOptions: {  series: { pointRange:  3600 * 1000 } },
    			title : { text : '' },
    			xAxis : { type : 'datetime', tickWidth : 0, gridLineWidth : 1, title : { text : 'Date' } },
    			yAxis : { title : { text : '' } },
    			legend : { align : 'left', verticalAlign : 'top', y : 10, floating : true, borderWidth : 0 },
    			exporting: {     buttons: { contextButton: {  text: 'Export' } }, sourceHeight:1050, sourceWidth: 1485    },
    			credits: { enabled: false }
    			// tooltip: { formatter: function() { return '<b>'+ this.series.name +'</b><br/>'+ Highcharts.dateFormat('%e. %b', this.x) +': '+ this.y +' m';} },
    	}; 
    	
    	$.getJSON("CopyOverflow", {overflowtime:$("#idOverflowTimeScale option:selected").val(), overflowplot:$("#idOverflowPlotSelect option:selected").val(), overflowdest:$("#idOverflowDestinationSelect option:selected").val()}, function(data) {
    		options.series=data.plot;
    		chart = new Highcharts.Chart(options); 
    		$('#overflowtablespace').dataTable( {
		    	"bJQueryUI": true,
		        "data": data.tableData.data,
		        "columns": data.tableData.headers,
		        "bDestroy": true
		    } );    
		    
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
				<li><a href="#tabs-9">Docs and links</a></li>
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

			<div id="tabs-3"><select id="idMapTimeScale">
				<option value="destination">show rates to site</option>
				<option value="source">show rates from site</option>
				</select> 
				<div id="map-canvas" style="height: 640px"></div>
			</div>
			<div id="tabs-4">
				Source <select id="idSource"></select> 
				Destination <select	id="idDestination"></select>
				Time bin:<select id="idTimeZoom">
				<option value="FAXcost">no Binning</option>
				<option value="FAXcost30min"  selected>30 min</option>
				<option value="FAXcost1h">1h</option>
				<option value="FAXcost3h">3h</option>
				<option value="FAXcost6h">6h</option>
				<option value="FAXcost12h">12h</option>
				<option value="FAXcost24h">24h</option>
				</select> 
				<br>
				<div id="graphspace"></div>
			</div>			
			<div id="tabs-5">
				Time scale:<select id="idCostMatrixTimeScale">
				<option value=6 selected>6h</option>
				<option value=12>12h</option>
				<option value=24>24h </option>
				<option value=48>48h</option>
				<option value=168>last week</option>
				<option value=336>last 2 weeks</option>
				<option value=672>last month</option>
				<option value=8760>last year</option>
				<option value=99999>all the time</option>
				</select> 
				<div id="costmatrixspace" style="height:1050px"></div>
			</div>	
			<div id="tabs-6">
				Test type:<select id="idStability">
				<option value=direct selected>Direct</option>
				<option value=upstream>Upstream</option>
				<option value=downstream>Downstream</option>
				<option value=x509>x509</option>
				</select> 
				Time scale:<select id="idStabilityTimeScale">
				<option value=24 selected>24 h</option>
				<option value=48>48 h</option>
				<option value=168>week</option>
				<option value=336>two weeks</option>
				<option value=672>month</option>
				</select> 
				<div id="stabilityspace" style="height:1100px"></div>
			</div>
			<div id="tabs-7">
				Time scale:<select id="idFailoverTimeScale">
				<option value=6>6h</option>
				<option value=12>12h</option>
				<option value=24 selected>24h </option>
				<option value=48>48h</option>
				<option value=168>last week</option>
				<option value=336>last 2 weeks</option>
				<option value=672>last month</option>
				<option value=8760>last year</option>
				</select> 
				
				Queue:<select id="idFailoverSiteSelect"></select> 
				Show:<select id="idFailoverPlotSelect">
				<option selected>jobs</option>
				<option>Files</option>
				<option>Data size</option>
				<option>Transfer duration</option>
				</select> 
				<div id="failovergraphspace"></div>
				<br>
				<br>
				<table cellpadding="0" cellspacing="0" border="0" class="display"
					id="failovertablespace" width="100%">
					<thead></thead>
					<tbody></tbody>
				</table>
<!-- 				<table cellpadding="0" cellspacing="0" border="0" class="display"
					id="failoverspace" width="100%">
					<thead>
						<tr>
							<th></th>
							<th>Site</th>
							<th>Username</th>
							<th>PandaID</th>
							<th>Status</th>
							<th>Files with FAX</th>
							<th>Files without FAX</th>
							<th>Bytes with FAX</th>
							<th>Bytes without FAX</th>
							<th>Time to copy</th>
						</tr>
					</thead>
					<tbody></tbody>
				</table> -->
			</div>
			<div id="tabs-8">				
				Time scale:<select id="idOverflowTimeScale">
				<option value=6>6h</option>
				<option value=12>12h</option>
				<option value=24 selected>24h </option>
				<option value=48>48h</option>
				<option value=168>last week</option>
				<option value=336>last 2 weeks</option>
				<option value=672>last month</option>
				<option value=8760>last year</option>
				</select> 
				Destination:<select id="idOverflowDestinationSelect"></select> 
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
				
				<b>
				<a href="https://twiki.cern.ch/twiki/bin/view/AtlasComputing/AtlasXrootdSystems">Homepage</a> <br>
				<a href="https://its.cern.ch/jira/browse/FAX">Jira</a> <br>
				<a href="http://git.cern.ch/pubweb/FAX.git">Git repository</a> <br>
				</b>
				
				<H2>TWiki</H2>
					<a href="https://twiki.cern.ch/twiki/bin/view/AtlasComputing/FaxCoverage">Current coverage</a><br>
					<a href="https://twiki.cern.ch/twiki/bin/view/AtlasComputing/RemotelyObtained">Remotely Obtained XRootD information</a> 
				<H2>Site Status Board (SSB)</H2>
					<a href="http://dashb-atlas-ssb.cern.ch/dashboard/request.py/siteview#currentView=FAX+endpoints&fullscreen=true&highlight=false">Endpoints tests</a> *Direct, upstream, downstream, X509*<br>
   					<a href="http://dashb-atlas-ssb.cern.ch/dashboard/request.py/siteview#currentView=FAX+redirectors&fullscreen=true&highlight=false">Redirectors test</a> *Upstream, downstream*<br>
   					<a href="http://dashb-atlas-ssb.cern.ch/dashboard/request.py/siteview#currentView=FAX+cost+matrix&fullscreen=true&highlight=false">FAX cost matrix</a> *Transfer rates by xrdcp*  	
				
				<H2>XRootD based monitoring</H2>
				<a href="http://atl-prod07.slac.stanford.edu:8080/">MonaLisa FAX dashboard</a>  <br>
				<a href="http://dashb-atlas-xrootd-transfers.cern.ch/ui">Transfers dashboard</a>  <br>
       			<a href="http://dashb-ai-621.cern.ch/cosmic/DB_ML_Comparator/">ML DDM consistency checks</a>     
				
			</div>
		</div>

	</div>

	<div id="loading" style="display: none">
		<br> <br>Loading data. Please wait...<br> <br> 
		<img src="static/wait_animated.gif" alt="loading" />
	</div>
	
	
</body>
</html>