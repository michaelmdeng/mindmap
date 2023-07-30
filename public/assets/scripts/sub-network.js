async function fetchNetwork(pageName) {
  var isTag = window.location.pathname.includes('tags');
  if (isTag) {
    var url = `${window.location.origin}/network/tag/${pageName}`
  } else {
    var url = `${window.location.origin}/network/note/${pageName}`
  }
  return fetch(url, {
    method: 'GET',
    headers: {
        'Content-Type': 'application/json',
    },
  }).then(response => response.json())
}

async function initializeSubnetwork(pageName) {
  var network = await fetchNetwork(pageName);
  var nodes = network.nodes;
  var edges = network.edges;

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
        centralGravity: 5,
        gravitationalConstant: -20000,
        springConstant: 0.05,
        springLength: 50,
        avoidOverlap: 0.75,
        damping: 0.25,
      },
      maxVelocity: 100,
      minVelocity: 2.5,
      stabilization: {
        iterations: 500,
      },
    },
  };

  var container = document.getElementById('note-network');
  var network = new vis.Network(container, data, options);

  network.on("doubleClick", function (obj) {
    var nodeId = obj.nodes[0];
    var node = nodes.find(n => n.id === nodeId)
    if (!node) {
      return;
    }

    var title = node.label;
    if (node.group === 'tag') {
      window.location = new URL('/tags/' + title, window.location.origin)
      return;
    }

    window.location = new URL('/notes/' + title, window.location.origin)
  });

  network.on("stabilized", function (obj) {
    network;
  });

  return network;
}

var pageName = document.getElementsByClassName('post-title').item(0).textContent.split('.')[0]
var network = await initializeSubnetwork(pageName);
