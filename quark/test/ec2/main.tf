// file: main.tf

provider "aws" {
  region = "${var.region}"
}

variable "region" {
  description = "the ID of the EC2 region."
  default     = "us-east-1"
}

variable "image_id" {
  description = "the AMI ID for the EC2 instance."
  default     = "ami-518bfb3b"
}

variable "ssh_username" {
  description = "the SSH username"
  default     = "fedora"
}

variable "ssh_key_name" {
  description = "the SSH key name"
}

variable "ssh_private_key" {
  description = "the SSH key name"
}

variable "ssh_public_key" {
  description = "the SSH key name"
}

resource "aws_key_pair" "main" {
  key_name   = "${var.ssh_key_name}"
  public_key = "${file(var.ssh_public_key)}"
}

resource "aws_security_group" "main" {
  name = "discovery-quark-ec2-integ"

  egress {
    cidr_blocks = ["0.0.0.0/0"]
    from_port   = 0
    protocol    = -1
    to_port     = 0
  }

  ingress {
    cidr_blocks = ["0.0.0.0/0"]
    from_port   = 0
    protocol    = -1
    to_port     = 0
  }
}

resource "aws_instance" "main" {
  ami           = "${var.image_id}"
  instance_type = "t2.nano"
  vpc_security_group_ids = ["${aws_security_group.main.id}"]
  key_name = "${aws_key_pair.main.key_name}"

  tags {
    Name = "discovery-quark-ec2-integ"
  }

  connection {
    user = "${var.ssh_username}"
    private_key = "${file(var.ssh_private_key)}"
  }

  // handles the polling logic for SSH availability.
  provisioner "remote-exec" {
    inline = ["# Connected!"]
  }
}

output "ssh_username" { value = "${var.ssh_username}" }

output "public_dns"   { value = "${aws_instance.main.public_dns}" }
output "public_ip"    { value = "${aws_instance.main.public_ip}" }

