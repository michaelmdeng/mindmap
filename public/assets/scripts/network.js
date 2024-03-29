async function fetchNetwork() {
  return fetch(`${window.location.origin}/network`, {
    method: 'GET',
    headers: {
        'Content-Type': 'application/json',
    },
  }).then(response => response.json())
}

async function fetchNetworkConfig() {
  return fetch(`${window.location.origin}/config/network`, {
    method: 'GET',
    headers: {
        'Content-Type': 'application/json',
    },
  }).then(response => response.json())
}

async function initializeNetwork() {
  var network = await fetchNetwork();
  var config = await fetchNetworkConfig();
  var nodes = network.nodes;
  var edges = network.edges;

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
        gravitationalConstant: -30000,
        springConstant: 0.01,
        springLength: 250,
        avoidOverlap: 0.5,
        damping: 0.20,
      },
      maxVelocity: 100,
      minVelocity: 2.5,
      stabilization: {
        iterations: 500,
      },
    },
  };

  var container = document.getElementById('mynetwork');
  var network = new vis.Network(container, data, options);
  var options = {
    joinCondition: function(parentNodeOptions, childNodeOptions) {
      var node = nodes.find(n => n.id === parentNodeOptions.id);
      return !network.clustering.isCluster(childNodeOptions.id) && parentNodeOptions.group === "tag" && !config.excludeClusterTags.some(tag => tag.name == node.label);
    },
    processProperties: generateCluster,
  }
  network.clustering.clusterByHubsize(config.clusterThreshold, options);

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

  network.on("stabilized", function (obj) {
    network;
  });

  return network;
}

function generateCluster(clusterOptions, childNodes, childEdges) {
  tagNode = childNodes.find(node => node.group === "tag");
  clusterOptions.label = tagNode.label;
  clusterOptions.color = '#74c0fc';
  clusterOptions.font = {
    color: '#333',
  };

  return clusterOptions;
}

var res =  initializeNetwork();
