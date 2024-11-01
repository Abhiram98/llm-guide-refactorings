## SETUP new server (one time at the start)

1. Create instance from machine image "jmove-kafka-placeholder"
2. SSH into machine
3. Mount the disk (if this gives an error about already being mounted, ignore it)
	sudo mount -o discard,defaults /dev/disk/by-id/google-disk-2 /mnt/disks/google_disk_2/
4. Connect to GUI:
	Follow step 3-6 in this link: https://cloud.google.com/architecture/chrome-desktop-remote-on-compute-engine?hl=en#configure_and_start_the_chrome_remote_desktop_service
5. give yourself write access to /home/abbe2165
	from the terminal, execute:
	sudo chmod a+w /home/abbe2165 


## Launching Jmove

Primary Eclipse- the IDE that has the Jmove code.
Secondary Eclipse - the IDE launch from the primary eclipse, on which you can actually trigger the JMove plugin.


1. Go to /home/abbe2165/desktop/
2. Click on Eclipse IDE for java developers.
3. Use the workspace /home/anne2165/eclipse-workspace
4. Jmove project should already be open, but if not, open it from 
	/mnt/disks/google_disk_2/jmove



__LIST OF COMPLETED DATA POINTS__
{567, 568, 569, 572, 581}
__END LIST OF COMPLETED DATA POINTS__


__IN-PROGRESS DATA POINTS__
{574}
__IN-PROGRESS DATA POINTS__


__PLAUSIBLE DATA POINTS__
{...}
__PLAUSIBLE DATA POINTS__


^ these are data points for which we _could_ expect jmove to execute and produce results. If there are eclipse setup issues (some red stuff in the code), jmove will not produce any results and there is no point running jmove on that datapoint.

## Compute list of plausible datapoints.
1. Checkout the branch of kafka.	
	- the branch name will be in the dataset. check "extraction_results.newBranchName"
	- checkout the branch using this command
	- cd /mnt/disks/google_disk_2/eclipse-jmove-3/kafka
	- git checkout <branchName> 
		If the branch doesn't exists, please perform `git fetch` on the repo, or ask Abhiram.
2. Open the _relavant_ file in the secondary Eclipse to check if it is successfully compiled by eclipse.
	- "move_method_refactoring.leftSideLocations[0].filePath"
	- Check that the IDE has no complaints, errors (red stuff) in the file
3. If there are no errors, add the ref_id to the list of plausible data points
4. If there are errors, note down that there was an error for this ref_id


## Running new data point

1. Get an ID from the plausible data point, checkout the branch (see previous step)
2. Execute Jmove by clicking the "i". Execute it on the highest project containing the file
3. Wait ~3hrs to let it finish. If not yet complete, cancel it and note it down.
4. **IMPORTANT** save the results. 
	- in the secondary IDE, do not click anywhere else, this will loose the results
	- press the save icon.
	- save file, in /mnt/disks/google_disk_2/jmove_results/ref<ref_id>.txt
	- write in the file how long it took to run jmove
	- note down that the refID has been completed.

