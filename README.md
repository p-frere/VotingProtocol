# VotingProtocol
An example voting protocol to show how consensus is reached with multiple clients

A consensus protocol that tolerates participant failures. 
The protocol involves two types of processes: a coordinator, whose role is to initiate a run of
the consensus algorithm and collect the outcome of the vote; and a participant, which contributes a
vote and communicates with the other participants to agree on an outcome. 

The application consists of 1 coordinator process and N participant processes, out of which at N-1 participants may
fail during the run of the consensus algorithm. The actual consensus algorithm is run among the
participant processes, with the coordinator only collecting outcomes from the participants.

The protocol is asynchronous. A series of possible votes are sent to all participants. The participants then exchange knowledge 
of votes. This is repeated until the information received for each node has been unchanged for 2 rounds.
Each client then returns its result and the coordinator confirms the result. 
