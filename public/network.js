var container = document.getElementById('mynetwork');

var nodeData = new vis.DataSet(nodes);
var edgeData = new vis.DataSet(edges);
var data = {
  nodes: nodeData,
  edges: edgeData
};

var options = {
  interaction: {
    hideEdgesOnZoom: true,
  },
  layout: {
    improvedLayout: false,
  },
  physics: {
    barnesHut: {
      centralGravity: 0.01,
      gravitationalConstant: -10000,
      springConstant: 0.015,
      springLength: 300,
      avoidOverlap: 0.5,
    },
  },
};

var network = new vis.Network(container, data, options);

network.on("doubleClick", function (obj) {
  var nodeId = obj.nodes[0];
  if (!clusterTags.hasOwnProperty(String(nodeId))) {
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
