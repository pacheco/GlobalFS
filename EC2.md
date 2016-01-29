# Distributed deployment on EC2

The fabric scripts `fabfile-ec2.py` and `fabfile-ec2deploy.py` were used to deploy on ec2.
They assume the VMs have been setup as per REQUIREMENTS.md.
They *most likely* need to be adapted to work for you.

The following is the steps I use which work on my EC2 setup:

    # choose/create a deployment on deployment.py (from now on called DEP)
    fab -f fabfile-ec2.py image_distribute:DEP,image_name
    fab -f fabfile-ec2.py head_start:DEP
    fab -f fabfile-ec2.py inst_start:DEP,image_name,token
    # wait instances to spin up through ec2 console... you should be able to ssh
    fab -f fabfile-ec2.py inst_config:DEP

    # put newest version in the 'head' machine then:
    fab -f fabfile-ec2deploy.py rsync_from_head:DEP

    # starting the system...
    fab -f fabfile-ec2deploy.py start_all_popup:DEP

    # kill everything...
    fab -f fabfile-ec2deploy
