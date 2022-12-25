var container = document.getElementById('note-network');

var pageName = document.getElementsByClassName('post-title').item(0).textContent.split('.')[0]
var currentNode = nodes.find(node => node.label === pageName);
var adjacentEdges = edges.filter(edge => edge.to === currentNode.id || edge.from === currentNode.id);
var relevantNodeIds = [...new Set(adjacentEdges.flatMap(edge => [edge.to, edge.from]).concat([currentNode.id]))];
var relevantNodes = nodes.filter(node => relevantNodeIds.includes(node.id)).filter(node => node.group != 'cluster');
relevantNodes.forEach(node => {
  node.hidden = false;
  node.physics = true;
});
var relevantEdges = adjacentEdges.filter(edge => relevantNodeIds.includes(edge.to) || relevantNodeIds.includes(edge.from));
relevantEdges.forEach(edge => {
  edge.hidden = false;
  edge.physics = true;
});

var nodeData = new vis.DataSet(relevantNodes);
var edgeData = new vis.DataSet(adjacentEdges);
var data = {
  nodes: nodeData,
  edges: edgeData
};

var options = {
  edges: {
    color: '#8c8c8c',
    width: 2,
  },
  groups: {
    cluster: {
      color: '#74c0fc',
      font: {
        color: '#333',
      },
    },
    note: {
      shape: 'box',
      color: '#c7c7c7',
      font: {
        color: '#333',
      },
    },
    tag: {
      color: '#91A7FF',
      font: {
        color: '#333',
      },
    },
  },
  interaction: {
    hideEdgesOnDrag: true,
    hideEdgesOnZoom: true,
  },
  layout: {
    improvedLayout: false,
  },
  nodes: {
    color: '#1f242A',
    font: {
      color: '#adb5bd',
    },
  },
  physics: {
    barnesHut: {
      centralGravity: 1,
      gravitationalConstant: -20000,
      springConstant: 0.01,
      springLength: 50,
      avoidOverlap: 1,
    },
    maxVelocity: 100,
    minVelocity: 2.5,
    stabilization: {
      iterations: 100,
    },
  },
};

var network = new vis.Network(container, data, options);

network.on("doubleClick", function (obj) {
  var nodeId = obj.nodes[0];
  if (!clusterTags.hasOwnProperty(String(nodeId))) {
    var file = nodes.find(n => n.id === nodeId).label
    window.location = new URL('/notes/' + file, window.location.origin)
    return;
  }
});
