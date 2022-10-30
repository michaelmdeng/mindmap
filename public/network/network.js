var container = document.getElementById('mynetwork');

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
      centralGravity: 0.75,
      gravitationalConstant: -15000,
      springConstant: 0.015,
      springLength: 350,
      avoidOverlap: 0.6,
    },
    maxVelocity: 100,
    minVelocity: 2.5,
    stabilization: {
      iterations: 200,
    },
  },
};

var network = new vis.Network(container, data, options);

network.on("doubleClick", function (obj) {
  var nodeId = obj.nodes[0];
  if (!clusterTags.hasOwnProperty(String(nodeId))) {
    var file = nodes.find(n => n.id === nodeId).label
    window.location = new URL('notes/' + file, window.location.origin)
    return;
  }

  var otherNodeId = clusterTags[nodeId];

  nodeData.update({id: nodeId, hidden: true, physics: false});
  nodeData.update({id: otherNodeId, hidden: false, physics: true});

  if (clusterNotes.hasOwnProperty(String(nodeId))) {
    var clusterId = nodeId;
    clusterNotes[clusterId].map((noteId) => {
      nodeData.update({id: noteId, hidden: false, physics: true});

      var connEdgeIds = network.getConnectedEdges(noteId);
      connEdgeIds.map((connEdgeId) => {
        edgeData.update({id: connEdgeId, hidden: false, physics: true});
      });
    });
  } else {
    var clusterId = otherNodeId;
    var clusterConnEdgeIds = network.getConnectedEdges(clusterId);
    clusterConnEdgeIds.map((clusterConnEdgeId) => {
      edgeData.update({id: clusterConnEdgeId, hidden: false, physics: true});
    });

    clusterNotes[clusterId].map((noteId) => {
      nodeData.update({id: noteId, hidden: true, physics: false});
    });
  }
});
