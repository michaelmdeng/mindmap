var container = document.getElementById('mynetwork');

var clusterNodes = nodes.filter(node => node.group === "cluster");
var tagNodes = nodes.filter(node => node.group === "tag");
nodes = nodes.filter(node => node.group !== "cluster");
nodes.forEach(node => {
  node.hidden = false;
  node.physics = true;
})
edges.forEach(edge => {
  edge.hidden = false;
  edge.physics = true;
})
var nodeData = new vis.DataSet(nodes);
var edgeData = new vis.DataSet(edges);
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
      springConstant: 0.1,
      springLength: 250,
      avoidOverlap: 0.25,
    },
    maxVelocity: 100,
    minVelocity: 5,
    stabilization: {
      iterations: 500,
    },
  },
};

var network = new vis.Network(container, data, options);

function generateCluster(clusterOptions, childNodes, childEdges) {
  tagNode = childNodes.find(node => node.group === "tag");
  clusterOptions.label = tagNode.label;
  clusterOptions.color = '#74c0fc';
  clusterOptions.font = {
    color: '#333',
  };

  return clusterOptions;
}

var options = {
  joinCondition: function(parentNodeOptions, childNodeOptions) {
    return !network.clustering.isCluster(childNodeOptions.id) && parentNodeOptions.group === "tag";
  },
  processProperties: generateCluster,
}
network.clustering.clusterByHubsize(10, options);

network.on("doubleClick", function (obj) {
  var nodeId = obj.nodes[0];
  if (nodeId === undefined) {
    return;
  }

  var node = nodes.find(n => n.id === nodeId);
  if (node !== undefined && node.group === "note") {
    var file = nodes.find(n => n.id === nodeId).label
    window.location = new URL('notes/' + file, window.location.origin)
  } else if (node !== undefined && node.group === "tag") {
    var options = {
      joinCondition: function(parentNodeOptions, childNodeOptions) {
        return !network.clustering.isCluster(childNodeOptions.id);
      },
      processProperties: generateCluster,
    }
    network.clustering.clusterByConnection(nodeId, options);
  } else if (network.clustering.isCluster(nodeId)) {
    network.clustering.openCluster(nodeId, {})
  }
});