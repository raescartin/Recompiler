private void getSize1Nodes(Node node, HashSet<Node> fixedSize1Nodes) {
				if(node.parents.size()==1){
					if(node.parents.get(0).children.get(0)==node||node.parents.get(0).children.get(node.parents.get(0).children.size()-1)==node){
						fixedSize1Nodes.add(node);
					}
					
				}
				for(Node parent:node.parents){
					this.getSize1Nodes(parent, fixedSize1Nodes);
				}
				if(node.outOfInstance!=null){//the node is out of instance
					if(node.outOfInstance.definition.name=="nand"){//NAND //TODO: fix nand checking
						//this is out
						//expand fixed size 1 nodes
						if(fixedSize1Nodes.contains(node)||fixedSize1Nodes.contains(node.outOfInstance.in.get(0))||fixedSize1Nodes.contains(node.outOfInstance.in.get(1))){
							fixedSize1Nodes.add(node);
							fixedSize1Nodes.add(node.outOfInstance.in.get(0));
							fixedSize1Nodes.add(node.outOfInstance.in.get(1));
						}
					}else{//the node is out of an instance different to NAND
						if(this.instances.contains(node.outOfInstance)){//check definition has not been removed (= is recursive)
							HashSet<Node> tempFixedSize1Nodes = new HashSet<Node>();
							//map outs
							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
								if(fixedSize1Nodes.contains(node.outOfInstance.out.get(i))){
									tempFixedSize1Nodes.add(node.outOfInstance.definition.out.get(i));
								}
							}
							//map ins
							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
								if(fixedSize1Nodes.contains(node.outOfInstance.in.get(i))){
									tempFixedSize1Nodes.add(node.outOfInstance.definition.in.get(i));
								}
							}
							//expand definition
							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
								node.outOfInstance.definition.getSize1Nodes(node.outOfInstance.definition.out.get(i), tempFixedSize1Nodes);
							}
							//expand outs if needed
							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
								if(tempFixedSize1Nodes.contains(node.outOfInstance.definition.out.get(i))){
									fixedSize1Nodes.add(node.outOfInstance.out.get(i));
								}
							}
							//expand ins if needed
							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
								if(tempFixedSize1Nodes.contains(node.outOfInstance.definition.in.get(i))){
									fixedSize1Nodes.add(node.outOfInstance.in.get(i));
								}
							}
						}
					}
				}
				if(node.inOfInstances!=null){//the node is in of instance(s)
					for(Instance instance:node.inOfInstances){
						if(this.instances.contains(instance)){//check definition has not been removed (= is recursive)
							if(instance.definition.name=="nand"){//NAND //TODO: fix nand checking
								//expand fixed size 1 nodes
								if(fixedSize1Nodes.contains(instance.out.get(0))||fixedSize1Nodes.contains(instance.in.get(0))||fixedSize1Nodes.contains(instance.in.get(1))){
									fixedSize1Nodes.add(instance.out.get(0));
									fixedSize1Nodes.add(instance.in.get(0));
									fixedSize1Nodes.add(instance.in.get(1));
								}
							}else{//the node is in of an instance different to NAND
								HashSet<Node> tempFixedSize1Nodes = new HashSet<Node>();
								//map outs
								for (int i = 0; i < instance.out.size(); i++) {
									if(fixedSize1Nodes.contains(instance.out.get(i))){
										tempFixedSize1Nodes.add(instance.definition.out.get(i));
									}
								}
								//map ins
								for (int i = 0; i < instance.in.size(); i++) {
									if(fixedSize1Nodes.contains(instance.in.get(i))){
										tempFixedSize1Nodes.add(instance.definition.in.get(i));
									}
								}
								//expand definition
								for (int i = 0; i < instance.out.size(); i++) {
									instance.definition.getSize1Nodes(instance.definition.out.get(i), tempFixedSize1Nodes);
								}
								//expand outs if needed
								for (int i = 0; i < instance.out.size(); i++) {
									if(tempFixedSize1Nodes.contains(instance.definition.out.get(i))){
										fixedSize1Nodes.add(instance.out.get(i));
									}
								}
								//expand ins if needed
								for (int i = 0; i < instance.in.size(); i++) {
									if(tempFixedSize1Nodes.contains(instance.definition.in.get(i))){
										fixedSize1Nodes.add(instance.in.get(i));
									}
								}
								
							}
						}
					}
				}
				
			}